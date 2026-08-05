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

/** Sessão de suporte remoto - superadmin actua como owner num tenant. */
@Entity
@Table(name = "remote_sessions")
@Getter
@Setter
public class RemoteSession extends BaseEntity {

    @Column(name = "platform_admin_id", nullable = false)
    private UUID platformAdminId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Staff OWNER sob cuja identidade a sessão opera (auditoria tenant-side). */
    @Column(name = "acting_staff_id", nullable = false)
    private UUID actingStaffId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
