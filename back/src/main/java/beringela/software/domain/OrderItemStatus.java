package beringela.software.domain;

public enum OrderItemStatus {
    /** Apontado pelo empregado; ainda não visível na cozinha. */
    DRAFT,
    PENDING,
    IN_PREPARATION,
    READY,
    SERVED,
    CANCELLED
}
