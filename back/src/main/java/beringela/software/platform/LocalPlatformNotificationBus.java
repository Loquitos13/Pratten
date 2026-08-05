package beringela.software.platform;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalPlatformNotificationBus implements PlatformNotificationBus {

    private final PlatformNotificationLocalRegistry registry;

    public LocalPlatformNotificationBus(PlatformNotificationLocalRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void broadcast(String notificationJson) {
        registry.pushNotificationJson(notificationJson);
    }
}
