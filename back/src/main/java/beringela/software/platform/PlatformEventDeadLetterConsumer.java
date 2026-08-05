package beringela.software.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Consome mensagens platform que esgotaram retries e foram para a DLQ. */
@Component
@ConditionalOnProperty(prefix = "pratten.messaging.rabbit", name = "enabled", havingValue = "true")
public class PlatformEventDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventDeadLetterConsumer.class);

    @RabbitListener(queues = RabbitConfig.PLATFORM_DLQ)
    public void consume(org.springframework.amqp.core.Message message) {
        log.error("Platform event moved to DLQ: routingKey={} body={}",
                message.getMessageProperties().getReceivedRoutingKey(),
                new String(message.getBody()));
    }
}
