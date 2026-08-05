package beringela.software.domain;

import beringela.software.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Snapshot da saúde operacional de um tenant (actualizado de forma assíncrona). */
@Entity
@Table(name = "tenant_health_snapshots")
@Getter
@Setter
public class TenantHealthSnapshot extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantHealthStatus status = TenantHealthStatus.UNKNOWN;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "active_sync_connections", nullable = false)
    private int activeSyncConnections;

    @Column(name = "avg_latency_ms", nullable = false)
    private long avgLatencyMs;

    @Column(name = "pending_sync_events", nullable = false)
    private long pendingSyncEvents;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;
}
