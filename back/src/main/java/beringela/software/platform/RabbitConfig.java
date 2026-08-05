package beringela.software.platform;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pratten.messaging.rabbit", name = "enabled", havingValue = "true")
public class RabbitConfig {

    public static final String PLATFORM_QUEUE = "pratten.platform.events";
    public static final String PLATFORM_DLX = "pratten.platform.dlx";
    public static final String PLATFORM_DLQ = "pratten.platform.events.dlq";
    public static final String PLATFORM_DLQ_ROUTING_KEY = "platform.events.dlq";

    @Bean
    DirectExchange platformExchange() {
        return new DirectExchange(RabbitPlatformEventPublisher.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange platformDeadLetterExchange() {
        return new DirectExchange(PLATFORM_DLX, true, false);
    }

    @Bean
    Queue platformQueue() {
        return QueueBuilder.durable(PLATFORM_QUEUE)
                .withArgument("x-dead-letter-exchange", PLATFORM_DLX)
                .withArgument("x-dead-letter-routing-key", PLATFORM_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue platformDeadLetterQueue() {
        return QueueBuilder.durable(PLATFORM_DLQ).build();
    }

    @Bean
    Binding platformBinding(Queue platformQueue, DirectExchange platformExchange) {
        return BindingBuilder.bind(platformQueue)
                .to(platformExchange)
                .with(RabbitPlatformEventPublisher.ROUTING_KEY);
    }

    @Bean
    Binding platformDeadLetterBinding(Queue platformDeadLetterQueue,
            DirectExchange platformDeadLetterExchange) {
        return BindingBuilder.bind(platformDeadLetterQueue)
                .to(platformDeadLetterExchange)
                .with(PLATFORM_DLQ_ROUTING_KEY);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
