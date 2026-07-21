package beringela.software.domain;

import beringela.software.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "restaurant_tables",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "number"}))
@Getter
@Setter
public class RestaurantTable extends TenantScopedEntity {

    @Column(nullable = false)
    private String number;

    private String zone;

    private int seats = 2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.FREE;

    /** Waiter responsible for this table, assigned by the manager. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_waiter_id")
    private StaffMember assignedWaiter;
}
