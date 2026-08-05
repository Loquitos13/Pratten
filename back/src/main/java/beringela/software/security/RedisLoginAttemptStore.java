package beringela.software.security;

import beringela.software.common.PrattenJson;
import beringela.software.redis.RedisChannelNames;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "true")
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private static final Duration ATTEMPT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public RedisLoginAttemptStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<AttemptState> find(String key) {
        String raw = redis.opsForValue().get(redisKey(key));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            AttemptState state = PrattenJson.read(raw, AttemptState.class);
            if (state.lockedUntil() != null && Instant.now().isAfter(state.lockedUntil())) {
                delete(key);
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (RuntimeException ex) {
            delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void save(String key, AttemptState state) {
        Duration ttl = state.lockedUntil() != null
                ? Duration.between(Instant.now(), state.lockedUntil()).plusSeconds(1)
                : ATTEMPT_TTL;
        if (ttl.isNegative() || ttl.isZero()) {
            delete(key);
            return;
        }
        redis.opsForValue().set(redisKey(key), PrattenJson.write(state), ttl);
    }

    @Override
    public void delete(String key) {
        redis.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return RedisChannelNames.LOGIN_KEY_PREFIX + key;
    }
}
