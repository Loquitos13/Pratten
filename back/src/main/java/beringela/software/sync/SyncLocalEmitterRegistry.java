package beringela.software.sync;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Registo local de emitters SSE por tenant (uma instância da app). */
@Component
public class SyncLocalEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SyncLocalEmitterRegistry.class);

    private final Map<UUID, List<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    public void register(UUID tenantId, SseEmitter emitter, Runnable onDisconnect) {
        List<SseEmitter> emitters =
                emittersByTenant.computeIfAbsent(tenantId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> disconnect(tenantId, emitter, onDisconnect));
        emitter.onTimeout(() -> disconnect(tenantId, emitter, onDisconnect));
        emitter.onError(ex -> disconnect(tenantId, emitter, onDisconnect));
    }

    public void fanOut(UUID tenantId, SyncEventType type, SyncEvent event) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(type.name()).data(event));
            } catch (IOException | IllegalStateException ex) {
                disconnect(tenantId, emitter, null);
            }
        }
    }

    public boolean hasLocalSubscribers(UUID tenantId) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        return emitters != null && !emitters.isEmpty();
    }

    private void disconnect(UUID tenantId, SseEmitter emitter, Runnable onDisconnect) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
        if (onDisconnect != null) {
            onDisconnect.run();
        }
        log.debug("Sync stream closed for tenant {}", tenantId);
    }
}
