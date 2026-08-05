package beringela.software.platform;

import beringela.software.domain.PlatformNotification;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.domain.TenantHealthSnapshot;
import beringela.software.domain.TenantHealthStatus;
import beringela.software.repository.PlatformNotificationRepository;
import beringela.software.repository.TenantHealthSnapshotRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.service.PlatformAlertDispatcher;
import beringela.software.service.PlatformNotificationStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Processa eventos platform: actualiza saúde, persiste notificações, push SSE. */
@Service
public class PlatformEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventProcessor.class);

    private final TenantHealthSnapshotRepository healthRepository;
    private final PlatformNotificationRepository notificationRepository;
    private final PlatformNotificationStreamService streamService;
    private final PlatformAlertDispatcher alertDispatcher;
    private final TenantRepository tenantRepository;

    public PlatformEventProcessor(TenantHealthSnapshotRepository healthRepository,
            PlatformNotificationRepository notificationRepository,
            PlatformNotificationStreamService streamService,
            PlatformAlertDispatcher alertDispatcher,
            TenantRepository tenantRepository) {
        this.healthRepository = healthRepository;
        this.notificationRepository = notificationRepository;
        this.streamService = streamService;
        this.alertDispatcher = alertDispatcher;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void process(PlatformEvent event) {
        if (event.tenantId() != null) {
            updateHealth(event);
        }
        PlatformNotification notification = toNotification(event);
        notificationRepository.save(notification);
        streamService.broadcast(notification);
        alertDispatcher.dispatch(event, notification);
        log.debug("Platform event processed: {} tenant={}", event.type(), event.tenantId());
    }

    private void updateHealth(PlatformEvent event) {
        TenantHealthSnapshot snapshot = healthRepository.findByTenantId(event.tenantId())
                .orElseGet(() -> {
                    TenantHealthSnapshot created = new TenantHealthSnapshot();
                    created.setTenantId(event.tenantId());
                    return created;
                });

        if (event.healthStatus() != null) {
            snapshot.setStatus(event.healthStatus());
        } else if (event.type() == PlatformEventType.TENANT_OFFLINE) {
            snapshot.setStatus(TenantHealthStatus.OFFLINE);
        } else if (event.type() == PlatformEventType.TENANT_DEGRADED
                || event.type() == PlatformEventType.HIGH_LATENCY
                || event.type() == PlatformEventType.SYNC_BACKLOG) {
            snapshot.setStatus(TenantHealthStatus.DEGRADED);
        } else if (event.type() == PlatformEventType.TENANT_RECOVERED) {
            snapshot.setStatus(TenantHealthStatus.HEALTHY);
        }

        if (event.avgLatencyMs() > 0) {
            snapshot.setAvgLatencyMs(event.avgLatencyMs());
        }
        if (event.activeSyncConnections() >= 0 && event.type() != PlatformEventType.TENANT_OFFLINE) {
            snapshot.setActiveSyncConnections(event.activeSyncConnections());
        }
        if (event.pendingSyncEvents() >= 0) {
            snapshot.setPendingSyncEvents(event.pendingSyncEvents());
        }
        if (event.type() == PlatformEventType.TENANT_OFFLINE
                || event.type() == PlatformEventType.TENANT_DEGRADED) {
            snapshot.setLastError(event.message());
        } else if (event.type() == PlatformEventType.TENANT_RECOVERED) {
            snapshot.setLastError(null);
        }
        snapshot.setLastCheckedAt(event.occurredAt());
        healthRepository.save(snapshot);
    }

    private PlatformNotification toNotification(PlatformEvent event) {
        PlatformNotification notification = new PlatformNotification();
        notification.setTenantId(event.tenantId());
        notification.setSeverity(event.severity());
        notification.setEventType(event.type().name());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setRead(false);

        if (event.tenantId() != null && event.tenantName() == null) {
            tenantRepository.findById(event.tenantId())
                    .ifPresent(t -> notification.setMessage(
                            "[" + t.getName() + "] " + event.message()));
        }
        return notification;
    }
}
