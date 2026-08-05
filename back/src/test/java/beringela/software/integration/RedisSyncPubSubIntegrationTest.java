package beringela.software.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import beringela.software.repository.TenantHealthSnapshotRepository;
import beringela.software.sync.RedisSyncEventBus;
import beringela.software.sync.SyncEventBus;
import beringela.software.sync.SyncEventType;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
class RedisSyncPubSubIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "pratten");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("pratten.redis.enabled", () -> "true");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "pratten");
    }

    @Autowired
    SyncEventBus syncEventBus;

    @Autowired
    TenantHealthSnapshotRepository healthRepository;

    @Test
    void redisPubSubRecordsBacklogWhenNoLocalSubscribers() {
        assertThat(syncEventBus).isInstanceOf(RedisSyncEventBus.class);

        UUID tenantId = UUID.randomUUID();
        syncEventBus.publish(tenantId, SyncEventType.ORDER_UPDATED, Map.of("test", true));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(healthRepository.findByTenantId(tenantId))
                        .isPresent()
                        .get()
                        .extracting(snapshot -> snapshot.getPendingSyncEvents())
                        .isEqualTo(1L));
    }
}
