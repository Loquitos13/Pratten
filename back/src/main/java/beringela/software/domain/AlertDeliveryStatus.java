package beringela.software.domain;

/** Estado de uma tentativa de entrega de alerta. */
public enum AlertDeliveryStatus {
    SENT,
    FAILED,
    SKIPPED
}
