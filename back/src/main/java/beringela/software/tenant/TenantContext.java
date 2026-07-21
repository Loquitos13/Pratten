package beringela.software.tenant;

import java.util.UUID;

/**
 * Holds the tenant currently bound to the executing thread. Populated by
 * {@link TenantFilter} on each request and consulted by Hibernate through
 * {@link beringela.software.config.TenantIdentifierResolver} so every query and
 * insert is automatically scoped to the active restaurant (tenant).
 */
public final class TenantContext {

    /** Sentinel tenant used when no tenant is bound (e.g. tenant registration). */
    public static final UUID SYSTEM_TENANT = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID require() {
        UUID tenantId = CURRENT.get();
        if (tenantId == null || SYSTEM_TENANT.equals(tenantId)) {
            throw new IllegalStateException("No tenant bound to the current request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
