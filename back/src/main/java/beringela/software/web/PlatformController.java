package beringela.software.web;

import beringela.software.dto.PlatformDtos.CreatePlatformTenantRequest;
import beringela.software.dto.PlatformDtos.PlatformAuditEntry;
import beringela.software.dto.PlatformDtos.PlatformDiagnostics;
import beringela.software.dto.PlatformDtos.PlatformTenantDetail;
import beringela.software.dto.PlatformDtos.PlatformTenantSummary;
import beringela.software.dto.PlatformDtos.ResetStaffPasswordRequest;
import beringela.software.dto.PlatformDtos.UpdatePlatformTenantRequest;
import beringela.software.security.AuthPrincipal;
import beringela.software.service.PlatformService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consola de plataforma - gestão de tenants e suporte remoto.
 *
 * <p>Para actuar nas APIs normais de um tenant (mesas, pedidos, etc.),
 * envia {@code X-Tenant-ID} com o JWT de superadmin.
 */
@RestController
@RequestMapping("/platform/tenants")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public List<PlatformTenantSummary> list() {
        return platformService.listTenants();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformTenantDetail create(@AuthenticationPrincipal AuthPrincipal admin,
            @Valid @RequestBody CreatePlatformTenantRequest request) {
        return platformService.createTenant(admin, request);
    }

    @GetMapping("/{tenantId}")
    public PlatformTenantDetail get(@PathVariable UUID tenantId) {
        return platformService.getTenant(tenantId);
    }

    @PatchMapping("/{tenantId}")
    public PlatformTenantDetail update(@AuthenticationPrincipal AuthPrincipal admin,
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdatePlatformTenantRequest request) {
        return platformService.updateTenant(admin, tenantId, request);
    }

    @GetMapping("/{tenantId}/diagnostics")
    public PlatformDiagnostics diagnostics(@PathVariable UUID tenantId) {
        return platformService.diagnostics(tenantId);
    }

    @GetMapping("/{tenantId}/audit")
    public List<PlatformAuditEntry> audit(@PathVariable UUID tenantId) {
        return platformService.auditForTenant(tenantId);
    }

    @PostMapping("/{tenantId}/staff/{staffId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal AuthPrincipal admin,
            @PathVariable UUID tenantId,
            @PathVariable UUID staffId,
            @Valid @RequestBody ResetStaffPasswordRequest request) {
        platformService.resetStaffPassword(admin, tenantId, staffId, request.password());
    }

    @PostMapping("/{tenantId}/staff/{staffId}/unlock-login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockLogin(@AuthenticationPrincipal AuthPrincipal admin,
            @PathVariable UUID tenantId,
            @PathVariable UUID staffId) {
        platformService.unlockStaffLogin(admin, tenantId, staffId);
    }
}
