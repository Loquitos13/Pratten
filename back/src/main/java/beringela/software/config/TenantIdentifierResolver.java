package beringela.software.config;

import beringela.software.tenant.TenantContext;
import java.util.UUID;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Resolves the active tenant for Hibernate's discriminator-based multi-tenancy.
 * Combined with the {@code @TenantId} field on tenant-scoped entities, Hibernate
 * transparently filters reads and stamps writes with the current tenant.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContext.get();
        return tenantId != null ? tenantId : TenantContext.SYSTEM_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(java.util.Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
