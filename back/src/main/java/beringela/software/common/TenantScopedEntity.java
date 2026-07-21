package beringela.software.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Base class for every entity that belongs to a single restaurant (tenant).
 * The {@link TenantId} field is populated and filtered automatically by
 * Hibernate based on the current {@link beringela.software.tenant.TenantContext}.
 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    public UUID getTenantId() {
        return tenantId;
    }
}
