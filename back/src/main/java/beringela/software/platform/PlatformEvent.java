package beringela.software.platform;

import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.domain.TenantHealthStatus;
import java.time.Instant;
import java.util.UUID;

/** Evento assíncrono processado pela fila platform (RabbitMQ ou local). */
public record PlatformEvent(
        PlatformEventType type,
        UUID tenantId,
        String tenantName,
        PlatformNotificationSeverity severity,
        TenantHealthStatus healthStatus,
        String title,
        String message,
        long avgLatencyMs,
        int activeSyncConnections,
        long pendingSyncEvents,
        Instant occurredAt) {

    public static PlatformEvent of(
            PlatformEventType type,
            UUID tenantId,
            String tenantName,
            PlatformNotificationSeverity severity,
            String title,
            String message) {
        return new PlatformEvent(
                type, tenantId, tenantName, severity, null,
                title, message, 0, 0, 0, Instant.now());
    }
}
