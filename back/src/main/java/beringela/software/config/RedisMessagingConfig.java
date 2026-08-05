package beringela.software.config;

import beringela.software.platform.PlatformNotificationRedisSubscriber;
import beringela.software.redis.RedisChannelNames;
import beringela.software.sync.SyncRedisSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class RedisMessagingConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SyncRedisSubscriber syncRedisSubscriber,
            PlatformNotificationRedisSubscriber platformNotificationRedisSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                syncRedisSubscriber, new ChannelTopic(RedisChannelNames.SYNC_EVENTS));
        container.addMessageListener(
                platformNotificationRedisSubscriber,
                new ChannelTopic(RedisChannelNames.PLATFORM_NOTIFICATIONS));
        return container;
    }
}
