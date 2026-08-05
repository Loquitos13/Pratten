package beringela.software.redis;

import java.util.UUID;

/** Contagem de ligações SSE activas (local ou agregada via Redis). */
public interface SyncConnectionTracker {

    void increment(UUID tenantId);

    void decrement(UUID tenantId);

    int count(UUID tenantId);
}
