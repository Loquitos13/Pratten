package beringela.software.sync;

import java.time.Instant;
import java.util.UUID;

/** Mensagem Pub/Sub para fan-out SSE entre instâncias. */
public record SyncBroadcastMessage(
        UUID tenantId,
        String eventType,
        String payloadJson,
        Instant at) {
}
