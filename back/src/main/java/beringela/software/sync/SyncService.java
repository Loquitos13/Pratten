package beringela.software.sync;

import beringela.software.tenant.TenantContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Fan-out hub that keeps every client of a restaurant in sync in real time.
 * Clients (waiter mobile app, POS, kitchen display, public website) open an SSE
 * stream per tenant; services broadcast changes (table freed/seated, new order,
 * reservation received) so all screens reflect the same state immediately.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<UUID, List<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    /** Opens a stream bound to the current tenant. */
    public SseEmitter subscribe() {
        UUID tenantId = TenantContext.require();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        List<SseEmitter> emitters =
                emittersByTenant.computeIfAbsent(tenantId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(tenantId, emitter));
        emitter.onTimeout(() -> remove(tenantId, emitter));
        emitter.onError(ex -> remove(tenantId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            remove(tenantId, emitter);
        }
        return emitter;
    }

    /** Broadcasts an event to every client of the current tenant. */
    public void publish(SyncEventType type, Object payload) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        publish(tenantId, type, payload);
    }

    public void publish(UUID tenantId, SyncEventType type, Object payload) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        SyncEvent event = SyncEvent.of(type, payload);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type.name()).data(event));
            } catch (IOException | IllegalStateException ex) {
                remove(tenantId, emitter);
            }
        }
    }

    private void remove(UUID tenantId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
        log.debug("Sync stream closed for tenant {}", tenantId);
    }
}
