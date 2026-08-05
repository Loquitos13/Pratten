package beringela.software.platform;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Publica eventos para RabbitMQ (produção / multi-instância). */
@Component
@ConditionalOnProperty(prefix = "pratten.messaging.rabbit", name = "enabled", havingValue = "true")
public class RabbitPlatformEventPublisher implements PlatformEventPublisher {

    public static final String EXCHANGE = "pratten.platform";
    public static final String ROUTING_KEY = "platform.events";

    private final RabbitTemplate rabbitTemplate;

    public RabbitPlatformEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(PlatformEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    @Component
    @ConditionalOnProperty(prefix = "pratten.messaging.rabbit", name = "enabled", havingValue = "true")
    static class RabbitPlatformEventConsumer {

        private final PlatformEventProcessor processor;

        RabbitPlatformEventConsumer(PlatformEventProcessor processor) {
            this.processor = processor;
        }

        @RabbitListener(queues = RabbitConfig.PLATFORM_QUEUE)
        public void consume(PlatformEvent event) {
            processor.process(event);
        }
    }
}
