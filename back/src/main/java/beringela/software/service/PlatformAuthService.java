package beringela.software.service;

import beringela.software.common.TooManyAttemptsException;
import beringela.software.domain.PlatformAdmin;
import beringela.software.dto.PlatformDtos.PlatformAuthResponse;
import beringela.software.dto.PlatformDtos.PlatformLoginRequest;
import beringela.software.dto.PlatformDtos.PlatformMeResponse;
import beringela.software.repository.PlatformAdminRepository;
import beringela.software.security.AuthPrincipal;
import beringela.software.security.JwtService;
import beringela.software.security.LoginAttemptService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformAuthService {

    private final PlatformAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    public PlatformAuthService(PlatformAdminRepository adminRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            LoginAttemptService loginAttempts) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    public PlatformAuthResponse login(PlatformLoginRequest request) {
        String key = "platform:" + request.email().toLowerCase(Locale.ROOT);
        if (loginAttempts.isBlocked(key)) {
            throw new TooManyAttemptsException(
                    "Demasiadas tentativas. Tenta novamente mais tarde.");
        }

        PlatformAdmin admin = adminRepository.findByEmailIgnoreCase(request.email())
                .filter(PlatformAdmin::isActive)
                .orElse(null);

        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            loginAttempts.recordFailure(key);
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttempts.reset(key);
        Instant expiresAt = jwtService.expiresAt();
        String token = jwtService.generatePlatform(admin, expiresAt);
        return PlatformAuthResponse.of(token, expiresAt, PlatformMeResponse.from(admin));
    }

    @Transactional(readOnly = true)
    public PlatformMeResponse me(AuthPrincipal principal) {
        PlatformAdmin admin = adminRepository.findById(principal.userId())
                .filter(PlatformAdmin::isActive)
                .orElseThrow(() -> new BadCredentialsException("Unknown admin"));
        return PlatformMeResponse.from(admin);
    }
}
