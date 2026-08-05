package beringela.software.web;

import beringela.software.dto.PlatformDtos.RemoteSessionResponse;
import beringela.software.dto.PlatformDtos.StartRemoteSessionRequest;
import beringela.software.security.AuthPrincipal;
import beringela.software.service.RemoteSessionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessões remotas de suporte - superadmin entra como OWNER num tenant.
 *
 * <p>O token devolvido substitui o JWT de platform nas APIs do restaurante
 * ({@code /tables}, {@code /orders}, etc.) até expirar ou ser encerrado.
 */
@RestController
@RequestMapping("/platform/tenants/{tenantId}/remote-session")
public class PlatformRemoteSessionController {

    private final RemoteSessionService remoteSessionService;

    public PlatformRemoteSessionController(RemoteSessionService remoteSessionService) {
        this.remoteSessionService = remoteSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemoteSessionResponse start(@AuthenticationPrincipal AuthPrincipal admin,
            @PathVariable UUID tenantId,
            @Valid @RequestBody StartRemoteSessionRequest request) {
        return remoteSessionService.start(admin, tenantId, request);
    }

    @PostMapping("/{sessionId}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@AuthenticationPrincipal AuthPrincipal admin,
            @PathVariable UUID tenantId,
            @PathVariable UUID sessionId) {
        remoteSessionService.end(admin, sessionId);
    }

    @GetMapping("/active")
    public List<RemoteSessionResponse> active(@PathVariable UUID tenantId) {
        return remoteSessionService.activeForTenant(tenantId);
    }
}
