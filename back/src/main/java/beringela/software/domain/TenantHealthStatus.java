package beringela.software.domain;

/** Estado operacional agregado de um tenant. */
public enum TenantHealthStatus {
    HEALTHY,
    DEGRADED,
    OFFLINE,
    ERROR,
    UNKNOWN
}
