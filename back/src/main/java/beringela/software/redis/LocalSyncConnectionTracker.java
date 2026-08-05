package beringela.software.redis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "pratten.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalSyncConnectionTracker implements SyncConnectionTracker {

    private final Map<UUID, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Override
    public void increment(UUID tenantId) {
        counts.computeIfAbsent(tenantId, id -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public void decrement(UUID tenantId) {
        counts.computeIfPresent(tenantId, (id, counter) -> {
            counter.updateAndGet(value -> Math.max(0, value - 1));
            return counter;
        });
    }

    @Override
    public int count(UUID tenantId) {
        AtomicInteger counter = counts.get(tenantId);
        return counter == null ? 0 : Math.max(0, counter.get());
    }
}
