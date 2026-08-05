package beringela.software.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Trava tentativas de login por conta, para conter força bruta.
 * Com Redis activo o estado é partilhado entre instâncias.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockout;
    private final LoginAttemptStore store;
    private final ApplicationEventPublisher events;

    public LoginAttemptService(
            @Value("${pratten.security.login.max-attempts:5}") int maxAttempts,
            @Value("${pratten.security.login.lockout-minutes:15}") long lockoutMinutes,
            LoginAttemptStore store,
            ApplicationEventPublisher events) {
        this.maxAttempts = maxAttempts;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
        this.store = store;
        this.events = events;
    }

    public boolean isBlocked(String key) {
        return store.find(key)
                .map(LoginAttemptStore.AttemptState::lockedUntil)
                .filter(until -> Instant.now().isBefore(until))
                .isPresent();
    }

    public void recordFailure(String key) {
        LoginAttemptStore.AttemptState existing = store.find(key).orElse(null);
        int count = (existing == null ? 0 : existing.count()) + 1;
        Instant until = count >= maxAttempts ? Instant.now().plus(lockout) : null;
        if (until != null && (existing == null || existing.lockedUntil() == null)) {
            String scope = key.startsWith("platform:") ? "platform" : "tenant";
            events.publishEvent(new LoginLockoutEvent(key, scope));
        }
        store.save(key, new LoginAttemptStore.AttemptState(count, until));
    }

    public void reset(String key) {
        store.delete(key);
    }
}
