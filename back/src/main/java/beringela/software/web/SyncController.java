package beringela.software.web;

import beringela.software.sync.SyncService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import beringela.software.service.TenantHealthService;
import beringela.software.tenant.TenantContext;
import java.util.UUID;

/**
 * Server-Sent Events stream that keeps mobile, POS, kitchen and the public
 * website continuously synchronized for a tenant.
 */
@RestController
@RequestMapping("/sync")
public class SyncController {

    private final SyncService syncService;
    private final TenantHealthService tenantHealthService;

    public SyncController(SyncService syncService, TenantHealthService tenantHealthService) {
        this.syncService = syncService;
        this.tenantHealthService = tenantHealthService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return syncService.subscribe();
    }

    /** Heartbeat periódico dos clientes - alimenta monitorização platform. */
    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody(required = false) SyncHeartbeatRequest request) {
        UUID tenantId = TenantContext.require();
        Long latency = request != null ? request.clientLatencyMs() : null;
        tenantHealthService.recordHeartbeat(tenantId, latency);
    }

    public record SyncHeartbeatRequest(
            @Min(0) @Max(600_000) Long clientLatencyMs) {
    }
}
