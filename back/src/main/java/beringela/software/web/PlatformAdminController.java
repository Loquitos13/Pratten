package beringela.software.web;

import beringela.software.dto.PlatformDtos.CreatePlatformAdminRequest;
import beringela.software.dto.PlatformDtos.PlatformAdminResponse;
import beringela.software.dto.PlatformDtos.ResetPlatformAdminPasswordRequest;
import beringela.software.dto.PlatformDtos.UpdatePlatformAdminRequest;
import beringela.software.security.AuthPrincipal;
import beringela.software.service.PlatformAdminService;
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

@RestController
@RequestMapping("/platform/admins")
public class PlatformAdminController {

    private final PlatformAdminService adminService;

    public PlatformAdminController(PlatformAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<PlatformAdminResponse> list() {
        return adminService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformAdminResponse create(@AuthenticationPrincipal AuthPrincipal actor,
            @Valid @RequestBody CreatePlatformAdminRequest request) {
        return adminService.create(actor, request);
    }

    @PatchMapping("/{id}")
    public PlatformAdminResponse update(@AuthenticationPrincipal AuthPrincipal actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformAdminRequest request) {
        return adminService.update(actor, id, request);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal AuthPrincipal actor,
            @PathVariable UUID id,
            @Valid @RequestBody ResetPlatformAdminPasswordRequest request) {
        adminService.resetPassword(actor, id, request);
    }
}
