package beringela.software.security;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Override
    public Optional<AttemptState> find(String key) {
        return Optional.ofNullable(attempts.get(key));
    }

    @Override
    public void save(String key, AttemptState state) {
        attempts.put(key, state);
    }

    @Override
    public void delete(String key) {
        attempts.remove(key);
    }
}
