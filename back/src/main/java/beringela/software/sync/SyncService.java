package beringela.software.sync;

import beringela.software.redis.SyncConnectionTracker;
import beringela.software.service.TenantHealthService;
import beringela.software.tenant.TenantContext;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Hub de sync em tempo real. SSE locais + Pub/Sub Redis entre instâncias.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final SyncLocalEmitterRegistry registry;
    private final SyncEventBus eventBus;
    private final SyncConnectionTracker connectionTracker;
    private final TenantHealthService tenantHealthService;

    public SyncService(SyncLocalEmitterRegistry registry,
            SyncEventBus eventBus,
            SyncConnectionTracker connectionTracker,
            TenantHealthService tenantHealthService) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.connectionTracker = connectionTracker;
        this.tenantHealthService = tenantHealthService;
    }

    public SseEmitter subscribe() {
        UUID tenantId = TenantContext.require();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        registry.register(tenantId, emitter, () -> onDisconnect(tenantId));
        connectionTracker.increment(tenantId);
        tenantHealthService.updateSyncConnectionCount(tenantId, connectionTracker.count(tenantId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            onDisconnect(tenantId);
        }
        return emitter;
    }

    public void publish(SyncEventType type, Object payload) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        publish(tenantId, type, payload);
    }

    public void publish(UUID tenantId, SyncEventType type, Object payload) {
        eventBus.publish(tenantId, type, payload);
    }

    private void onDisconnect(UUID tenantId) {
        connectionTracker.decrement(tenantId);
        tenantHealthService.updateSyncConnectionCount(tenantId, connectionTracker.count(tenantId));
        log.debug("Sync client disconnected tenant={}", tenantId);
    }
}
