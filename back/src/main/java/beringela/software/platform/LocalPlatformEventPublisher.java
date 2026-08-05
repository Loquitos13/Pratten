package beringela.software.platform;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fallback em dev/single-node: processa eventos inline sem broker externo. */
@Component
@ConditionalOnProperty(prefix = "pratten.messaging.rabbit", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalPlatformEventPublisher implements PlatformEventPublisher {

    private final PlatformEventProcessor processor;

    public LocalPlatformEventPublisher(PlatformEventProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void publish(PlatformEvent event) {
        processor.process(event);
    }
}
