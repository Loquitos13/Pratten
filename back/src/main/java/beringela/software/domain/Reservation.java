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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** A table booking, typically received from the restaurant's public website. */
@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservation extends TenantScopedEntity {

    @Column(nullable = false)
    private String customerName;

    private String customerPhone;

    private String customerEmail;

    @Column(nullable = false)
    private int partySize = 2;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(nullable = false)
    private int durationMinutes = 120;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationSource source = ReservationSource.WEBSITE;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;
}
