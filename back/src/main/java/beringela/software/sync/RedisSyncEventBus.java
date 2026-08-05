package beringela.software.sync;

import beringela.software.redis.RedisChannelNames;
import beringela.software.common.PrattenJson;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class RedisSyncEventBus implements SyncEventBus {

    private final StringRedisTemplate redis;

    public RedisSyncEventBus(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void publish(UUID tenantId, SyncEventType type, Object payload) {
        SyncBroadcastMessage message = new SyncBroadcastMessage(
                tenantId,
                type.name(),
                PrattenJson.write(payload),
                java.time.Instant.now());
        redis.convertAndSend(RedisChannelNames.SYNC_EVENTS, PrattenJson.write(message));
    }
}
