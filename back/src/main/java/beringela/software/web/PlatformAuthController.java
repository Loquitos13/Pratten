package beringela.software.web;

import beringela.software.dto.PlatformDtos.PlatformLoginRequest;
import beringela.software.dto.PlatformDtos.PlatformAuthResponse;
import beringela.software.dto.PlatformDtos.PlatformMeResponse;
import beringela.software.security.AuthPrincipal;
import beringela.software.service.PlatformAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/auth")
public class PlatformAuthController {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformAuthResponse login(@Valid @RequestBody PlatformLoginRequest request) {
        return platformAuthService.login(request);
    }

    @GetMapping("/me")
    public PlatformMeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return platformAuthService.me(principal);
    }
}
