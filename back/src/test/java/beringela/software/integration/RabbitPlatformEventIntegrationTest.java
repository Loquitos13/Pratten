package beringela.software.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventPublisher;
import beringela.software.platform.PlatformEventType;
import beringela.software.platform.RabbitPlatformEventPublisher;
import beringela.software.repository.PlatformNotificationRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "pratten.messaging.rabbit.enabled=true")
class RabbitPlatformEventIntegrationTest {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    PlatformEventPublisher eventPublisher;

    @Autowired
    PlatformNotificationRepository notificationRepository;

    @Test
    void eventIsPublishedAndProcessedThroughRabbitMq() {
        assertThat(eventPublisher).isInstanceOf(RabbitPlatformEventPublisher.class);

        UUID tenantId = UUID.randomUUID();
        eventPublisher.publish(PlatformEvent.of(
                PlatformEventType.LOW_STOCK,
                tenantId,
                "Tenant IT",
                PlatformNotificationSeverity.WARNING,
                "Stock baixo",
                "Bacalhau abaixo do mínimo"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(notificationRepository.findAll()).anyMatch(notification ->
                        "LOW_STOCK".equals(notification.getEventType())
                                && notification.getMessage().contains("Bacalhau")));
    }
}
