package beringela.software.security;

import java.time.Instant;
import java.util.Optional;

/** Estado partilhado de tentativas de login (in-memory ou Redis). */
public interface LoginAttemptStore {

    Optional<AttemptState> find(String key);

    void save(String key, AttemptState state);

    void delete(String key);

    record AttemptState(int count, Instant lockedUntil) {
    }
}
