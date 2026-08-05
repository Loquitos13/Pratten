package beringela.software.platform;

/** Tipos de eventos operacionais publicados para a consola platform. */
public enum PlatformEventType {
    TENANT_OFFLINE,
    TENANT_DEGRADED,
    TENANT_RECOVERED,
    HIGH_LATENCY,
    SYNC_BACKLOG,
    LOGIN_LOCKOUT_SPIKE,
    REMOTE_SESSION_STARTED,
    REMOTE_SESSION_ENDED,
    TENANT_INACTIVE,
    LOW_STOCK
}
