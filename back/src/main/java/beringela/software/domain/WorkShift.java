package beringela.software.domain;

import beringela.software.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Período em que um empregado está em horário de trabalho (clock in/out). */
@Entity
@Table(name = "work_shifts")
@Getter
@Setter
public class WorkShift extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_member_id", nullable = false)
    private StaffMember staffMember;

    @Column(name = "clock_in_at", nullable = false)
    private Instant clockInAt;

    @Column(name = "clock_out_at")
    private Instant clockOutAt;

    @Column(name = "clock_in_notes", length = 500)
    private String clockInNotes;

    @Column(name = "clock_out_notes", length = 500)
    private String clockOutNotes;

    public boolean isActive() {
        return clockOutAt == null;
    }
}
