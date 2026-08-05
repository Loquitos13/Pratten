package beringela.software.domain;

import beringela.software.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Registo de acções do superadmin (suporte remoto). */
@Entity
@Table(name = "platform_audit_logs")
@Getter
@Setter
public class PlatformAuditLog extends BaseEntity {

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 1000)
    private String detail;
}
