package beringela.software.sync;

import java.time.Instant;

/**
 * A real-time change broadcast to every client (mobile, POS, kitchen, website)
 * connected to a tenant's sync stream.
 */
public record SyncEvent(SyncEventType type, Object payload, Instant at) {

    public static SyncEvent of(SyncEventType type, Object payload) {
        return new SyncEvent(type, payload, Instant.now());
    }
}
