package beringela.software.redis;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class RedisSyncConnectionTracker implements SyncConnectionTracker {

    private final StringRedisTemplate redis;

    public RedisSyncConnectionTracker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void increment(UUID tenantId) {
        redis.opsForValue().increment(key(tenantId));
    }

    @Override
    public void decrement(UUID tenantId) {
        Long value = redis.opsForValue().decrement(key(tenantId));
        if (value != null && value < 0) {
            redis.opsForValue().set(key(tenantId), "0");
        }
    }

    @Override
    public int count(UUID tenantId) {
        String raw = redis.opsForValue().get(key(tenantId));
        if (raw == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String key(UUID tenantId) {
        return RedisChannelNames.SYNC_CONN_KEY_PREFIX + tenantId;
    }
}
