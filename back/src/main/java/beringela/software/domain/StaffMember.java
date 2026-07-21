package beringela.software.domain;

import beringela.software.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_members")
@Getter
@Setter
public class StaffMember extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    private String email;

    private String pin;

    /** BCrypt hash; present for members allowed to log in. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role = StaffRole.WAITER;

    @Column(nullable = false)
    private boolean active = true;
}
