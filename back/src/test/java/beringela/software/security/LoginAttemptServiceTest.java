package beringela.software.security;

import static org.assertj.core.api.Assertions.assertThat;

import beringela.software.security.InMemoryLoginAttemptStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(3, 1, new InMemoryLoginAttemptStore(), event -> { });
    }

    @Test
    void bloqueiaAposMaxTentativas() {
        String key = "demo:ana@demo.pt";

        service.recordFailure(key);
        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isFalse();

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isTrue();
    }

    @Test
    void resetRemoveBloqueio() {
        String key = "demo:ana@demo.pt";
        service.recordFailure(key);
        service.recordFailure(key);
        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isTrue();

        service.reset(key);
        assertThat(service.isBlocked(key)).isFalse();
    }
}
