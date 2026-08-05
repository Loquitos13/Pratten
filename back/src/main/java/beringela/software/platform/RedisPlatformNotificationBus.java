package beringela.software.platform;

import beringela.software.common.PrattenJson;
import beringela.software.redis.RedisChannelNames;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class RedisPlatformNotificationBus implements PlatformNotificationBus {

    private final StringRedisTemplate redis;

    public RedisPlatformNotificationBus(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void broadcast(String notificationJson) {
        PlatformNotificationMessage message = new PlatformNotificationMessage(notificationJson);
        redis.convertAndSend(
                RedisChannelNames.PLATFORM_NOTIFICATIONS, PrattenJson.write(message));
    }
}
