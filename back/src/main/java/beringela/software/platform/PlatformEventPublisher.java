package beringela.software.platform;

/** Publica eventos platform para processamento assíncrono (RabbitMQ ou inline). */
public interface PlatformEventPublisher {

    void publish(PlatformEvent event);
}
