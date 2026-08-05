package beringela.software.sync;

import beringela.software.service.TenantHealthService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fan-out apenas na instância local (dev / single-node). */
@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalSyncEventBus implements SyncEventBus {

    private final SyncLocalEmitterRegistry registry;
    private final TenantHealthService tenantHealthService;

    public LocalSyncEventBus(SyncLocalEmitterRegistry registry,
            TenantHealthService tenantHealthService) {
        this.registry = registry;
        this.tenantHealthService = tenantHealthService;
    }

    @Override
    public void publish(UUID tenantId, SyncEventType type, Object payload) {
        if (!registry.hasLocalSubscribers(tenantId)) {
            tenantHealthService.recordPendingEvents(tenantId, 1);
            return;
        }
        SyncEvent event = SyncEvent.of(type, payload);
        registry.fanOut(tenantId, type, event);
    }
}
