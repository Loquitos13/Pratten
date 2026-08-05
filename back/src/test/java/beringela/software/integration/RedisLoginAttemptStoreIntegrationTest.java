package beringela.software.integration;

import static org.assertj.core.api.Assertions.assertThat;

import beringela.software.security.LoginAttemptStore;
import beringela.software.security.LoginAttemptStore.AttemptState;
import beringela.software.security.RedisLoginAttemptStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class RedisLoginAttemptStoreIntegrationTest {

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
    LoginAttemptStore loginAttemptStore;

    @Test
    void persistsLockoutStateInRedis() {
        assertThat(loginAttemptStore).isInstanceOf(RedisLoginAttemptStore.class);

        String key = "demo:it-user@pratten.pt";
        AttemptState state = new AttemptState(5, Instant.now().plus(10, ChronoUnit.MINUTES));
        loginAttemptStore.save(key, state);

        assertThat(loginAttemptStore.find(key))
                .isPresent()
                .get()
                .extracting(AttemptState::count)
                .isEqualTo(5);

        loginAttemptStore.delete(key);
        assertThat(loginAttemptStore.find(key)).isEmpty();
    }
}
