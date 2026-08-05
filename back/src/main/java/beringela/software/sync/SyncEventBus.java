package beringela.software.sync;

import java.util.UUID;

/** Publica eventos de sync (local ou via Redis). */
public interface SyncEventBus {

    void publish(UUID tenantId, SyncEventType type, Object payload);
}
