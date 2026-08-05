package beringela.software.service;

import beringela.software.domain.Tenant;
import beringela.software.domain.TenantHealthSnapshot;
import beringela.software.domain.TenantHealthStatus;
import beringela.software.dto.PlatformDtos.TenantHealthResponse;
import beringela.software.dto.PlatformDtos.TenantHealthOverview;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventPublisher;
import beringela.software.platform.PlatformEventType;
import beringela.software.repository.TenantHealthSnapshotRepository;
import beringela.software.repository.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import beringela.software.domain.PlatformNotificationSeverity;

/** Monitoriza ligações SSE, latência e heartbeats de cada tenant. */
@Service
public class TenantHealthService {

    private final TenantHealthSnapshotRepository healthRepository;
    private final TenantRepository tenantRepository;
    private final PlatformEventPublisher eventPublisher;

    private final Map<UUID, AtomicLong> latencySumByTenant = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> latencyCountByTenant = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> pendingEventsByTenant = new ConcurrentHashMap<>();

    private final long offlineThresholdSeconds;
    private final long degradedLatencyMs;

    public TenantHealthService(
            TenantHealthSnapshotRepository healthRepository,
            TenantRepository tenantRepository,
            PlatformEventPublisher eventPublisher,
            @Value("${pratten.platform.health.offline-threshold-seconds:120}") long offlineThresholdSeconds,
            @Value("${pratten.platform.health.degraded-latency-ms:2000}") long degradedLatencyMs) {
        this.healthRepository = healthRepository;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.offlineThresholdSeconds = offlineThresholdSeconds;
        this.degradedLatencyMs = degradedLatencyMs;
    }

    public void updateSyncConnectionCount(UUID tenantId, int totalConnections) {
        TenantHealthSnapshot snapshot = getOrCreate(tenantId);
        snapshot.setActiveSyncConnections(Math.max(0, totalConnections));
        if (totalConnections > 0) {
            snapshot.setLastHeartbeat(Instant.now());
            if (snapshot.getStatus() == TenantHealthStatus.OFFLINE) {
                publishRecovery(tenantId);
            }
        }
        snapshot.setLastCheckedAt(Instant.now());
        healthRepository.save(snapshot);
    }

    public void recordHeartbeat(UUID tenantId, Long clientLatencyMs) {
        TenantHealthSnapshot snapshot = getOrCreate(tenantId);
        snapshot.setLastHeartbeat(Instant.now());
        snapshot.setLastCheckedAt(Instant.now());
        if (clientLatencyMs != null && clientLatencyMs > 0) {
            recordLatency(tenantId, clientLatencyMs);
            snapshot.setAvgLatencyMs(currentAvgLatency(tenantId));
        }
        if (snapshot.getStatus() == TenantHealthStatus.OFFLINE) {
            publishRecovery(tenantId);
        }
        healthRepository.save(snapshot);
    }

    public void recordLatency(UUID tenantId, long latencyMs) {
        latencySumByTenant.computeIfAbsent(tenantId, k -> new AtomicLong()).addAndGet(latencyMs);
        latencyCountByTenant.computeIfAbsent(tenantId, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordPendingEvents(UUID tenantId, long count) {
        pendingEventsByTenant.put(tenantId, new AtomicLong(count));
        TenantHealthSnapshot snapshot = getOrCreate(tenantId);
        snapshot.setPendingSyncEvents(count);
        healthRepository.save(snapshot);

        if (count > 50) {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            eventPublisher.publish(new PlatformEvent(
                    PlatformEventType.SYNC_BACKLOG,
                    tenantId,
                    tenant != null ? tenant.getName() : null,
                    PlatformNotificationSeverity.WARNING,
                    TenantHealthStatus.DEGRADED,
                    "Mensagens SSE atrasadas",
                    count + " eventos pendentes na fila de sync",
                    snapshot.getAvgLatencyMs(),
                    snapshot.getActiveSyncConnections(),
                    count,
                    Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public TenantHealthResponse healthForTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        TenantHealthSnapshot snapshot = healthRepository.findByTenantId(tenantId)
                .orElseGet(() -> emptySnapshot(tenantId));
        return TenantHealthResponse.from(tenant, snapshot);
    }

    @Transactional(readOnly = true)
    public TenantHealthOverview overview() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<TenantHealthResponse> items = tenants.stream()
                .map(t -> {
                    TenantHealthSnapshot s = healthRepository.findByTenantId(t.getId())
                            .orElseGet(() -> emptySnapshot(t.getId()));
                    return TenantHealthResponse.from(t, s);
                })
                .toList();

        long healthy = items.stream().filter(i -> i.status() == TenantHealthStatus.HEALTHY).count();
        long degraded = items.stream().filter(i -> i.status() == TenantHealthStatus.DEGRADED).count();
        long offline = items.stream().filter(i -> i.status() == TenantHealthStatus.OFFLINE).count();
        return new TenantHealthOverview(items.size(), healthy, degraded, offline, items);
    }

    /** Invocado periodicamente para detectar tenants offline ou degradados. */
    public void evaluateAllTenants() {
        Instant now = Instant.now();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.isActive()) {
                continue;
            }
            TenantHealthSnapshot snapshot = getOrCreate(tenant.getId());
            snapshot.setLastCheckedAt(now);

            if (snapshot.getLastHeartbeat() == null) {
                snapshot.setStatus(TenantHealthStatus.UNKNOWN);
                healthRepository.save(snapshot);
                continue;
            }

            long secondsSinceHeartbeat = now.getEpochSecond() - snapshot.getLastHeartbeat().getEpochSecond();
            TenantHealthStatus previous = snapshot.getStatus();

            if (secondsSinceHeartbeat > offlineThresholdSeconds) {
                snapshot.setStatus(TenantHealthStatus.OFFLINE);
                if (previous != TenantHealthStatus.OFFLINE) {
                    eventPublisher.publish(new PlatformEvent(
                            PlatformEventType.TENANT_OFFLINE,
                            tenant.getId(),
                            tenant.getName(),
                            PlatformNotificationSeverity.CRITICAL,
                            TenantHealthStatus.OFFLINE,
                            "Tenant offline",
                            "Sem heartbeat há " + secondsSinceHeartbeat + "s",
                            snapshot.getAvgLatencyMs(),
                            snapshot.getActiveSyncConnections(),
                            snapshot.getPendingSyncEvents(),
                            now));
                }
            } else if (snapshot.getAvgLatencyMs() > degradedLatencyMs) {
                snapshot.setStatus(TenantHealthStatus.DEGRADED);
                if (previous != TenantHealthStatus.DEGRADED) {
                    eventPublisher.publish(new PlatformEvent(
                            PlatformEventType.HIGH_LATENCY,
                            tenant.getId(),
                            tenant.getName(),
                            PlatformNotificationSeverity.WARNING,
                            TenantHealthStatus.DEGRADED,
                            "Latência elevada",
                            "Média " + snapshot.getAvgLatencyMs() + "ms (limiar " + degradedLatencyMs + "ms)",
                            snapshot.getAvgLatencyMs(),
                            snapshot.getActiveSyncConnections(),
                            snapshot.getPendingSyncEvents(),
                            now));
                }
            } else {
                snapshot.setStatus(TenantHealthStatus.HEALTHY);
            }
            healthRepository.save(snapshot);
        }
    }

    private void publishRecovery(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        eventPublisher.publish(PlatformEvent.of(
                PlatformEventType.TENANT_RECOVERED,
                tenantId,
                tenant != null ? tenant.getName() : null,
                PlatformNotificationSeverity.INFO,
                "Tenant recuperado",
                "Ligação restabelecida"));
    }

    private TenantHealthSnapshot getOrCreate(UUID tenantId) {
        return healthRepository.findByTenantId(tenantId).orElseGet(() -> {
            TenantHealthSnapshot created = emptySnapshot(tenantId);
            return healthRepository.save(created);
        });
    }

    private TenantHealthSnapshot emptySnapshot(UUID tenantId) {
        TenantHealthSnapshot snapshot = new TenantHealthSnapshot();
        snapshot.setTenantId(tenantId);
        snapshot.setStatus(TenantHealthStatus.UNKNOWN);
        return snapshot;
    }

    private long currentAvgLatency(UUID tenantId) {
        AtomicLong sum = latencySumByTenant.get(tenantId);
        AtomicLong count = latencyCountByTenant.get(tenantId);
        if (sum == null || count == null || count.get() == 0) {
            return 0;
        }
        return sum.get() / count.get();
    }
}
