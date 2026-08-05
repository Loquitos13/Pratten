package beringela.software.sync;

import beringela.software.service.TenantHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import beringela.software.common.PrattenJson;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class SyncRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(SyncRedisSubscriber.class);

    private final SyncLocalEmitterRegistry registry;
    private final TenantHealthService tenantHealthService;

    public SyncRedisSubscriber(SyncLocalEmitterRegistry registry,
            TenantHealthService tenantHealthService) {
        this.registry = registry;
        this.tenantHealthService = tenantHealthService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            SyncBroadcastMessage broadcast = PrattenJson.read(
                    new String(message.getBody()), SyncBroadcastMessage.class);
            SyncEventType type = SyncEventType.valueOf(broadcast.eventType());
            Object payload = PrattenJson.mapper().readTree(broadcast.payloadJson());
            SyncEvent event = new SyncEvent(type, payload, broadcast.at());

            if (!registry.hasLocalSubscribers(broadcast.tenantId())) {
                tenantHealthService.recordPendingEvents(broadcast.tenantId(), 1);
                return;
            }
            registry.fanOut(broadcast.tenantId(), type, event);
        } catch (Exception ex) {
            log.warn("Failed to process sync Redis message: {}", ex.getMessage());
        }
    }
}
