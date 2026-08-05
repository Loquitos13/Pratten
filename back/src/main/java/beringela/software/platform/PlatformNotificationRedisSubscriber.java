package beringela.software.platform;

import beringela.software.common.PrattenJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class PlatformNotificationRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(PlatformNotificationRedisSubscriber.class);

    private final PlatformNotificationLocalRegistry registry;

    public PlatformNotificationRedisSubscriber(PlatformNotificationLocalRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            PlatformNotificationMessage payload = PrattenJson.read(
                    new String(message.getBody()), PlatformNotificationMessage.class);
            registry.pushNotificationJson(payload.notificationJson());
        } catch (Exception ex) {
            log.warn("Failed to process platform notification Redis message: {}", ex.getMessage());
        }
    }
}
