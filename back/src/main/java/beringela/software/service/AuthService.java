package beringela.software.service;

import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.Tenant;
import beringela.software.dto.AuthDtos.AuthResponse;
import beringela.software.dto.AuthDtos.LoginRequest;
import beringela.software.dto.AuthDtos.MeResponse;
import beringela.software.dto.AuthDtos.RegisterRequest;
import beringela.software.common.TooManyAttemptsException;
import beringela.software.dto.TenantDtos.CreateTenantRequest;
import beringela.software.repository.TenantRepository;
import beringela.software.security.AuthPrincipal;
import beringela.software.security.JwtService;
import beringela.software.security.LoginAttemptService;
import beringela.software.tenant.TenantSessionExecutor;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Onboarding and login. Tenant-scoped work runs through {@link TenantSessionExecutor}
 * because the tenant is only known mid-request here, after the request-scoped
 * (open-in-view) session has already been opened under the sentinel tenant.
 */
@Service
public class AuthService {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final TenantSessionExecutor tenantSessions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    /** Hash válido usado para igualar o tempo quando a conta não existe. */
    private final String dummyHash;

    public AuthService(TenantService tenantService, TenantRepository tenantRepository,
            TenantSessionExecutor tenantSessions, PasswordEncoder passwordEncoder,
            JwtService jwtService, LoginAttemptService loginAttempts) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.tenantSessions = tenantSessions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest request) {
        Tenant tenant = tenantService.create(new CreateTenantRequest(
                request.restaurantName(), request.slug(), null, null, null));

        StaffMember owner = tenantSessions.inTenant(tenant.getId(), session -> {
            StaffMember staff = new StaffMember();
            staff.setName(request.managerName());
            staff.setEmail(request.email());
            staff.setRole(StaffRole.OWNER);
            staff.setPasswordHash(passwordEncoder.encode(request.password()));
            session.persist(staff);
            return staff;
        });
        return buildResponse(owner, tenant);
    }

    public AuthResponse login(LoginRequest request) {
        String key = attemptKey(request);
        if (loginAttempts.isBlocked(key)) {
            throw new TooManyAttemptsException(
                    "Demasiadas tentativas. Tenta novamente mais tarde.");
        }

        Tenant tenant = tenantRepository.findBySlug(request.slug())
                .filter(Tenant::isActive)
                .orElse(null);

        StaffMember user = tenant == null ? null : tenantSessions.inTenant(tenant.getId(), session ->
                session.createQuery(
                                "select s from StaffMember s where lower(s.email) = lower(:email)",
                                StaffMember.class)
                        .setParameter("email", request.email())
                        .getResultStream()
                        .findFirst()
                        .orElse(null));

        boolean valid = user != null && user.isActive() && user.getPasswordHash() != null
                && passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!valid) {
            // Corre sempre um bcrypt para não revelar por tempo se a conta existe.
            if (user == null || user.getPasswordHash() == null) {
                passwordEncoder.matches(request.password(), dummyHash);
            }
            loginAttempts.recordFailure(key);
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttempts.reset(key);
        return buildResponse(user, tenant);
    }

    private String attemptKey(LoginRequest request) {
        return (request.slug() + ':' + request.email()).toLowerCase(Locale.ROOT);
    }

    public MeResponse me(AuthPrincipal principal) {
        Tenant tenant = tenantService.get(principal.tenantId());
        StaffMember user = tenantSessions.inTenant(tenant.getId(), session ->
                session.find(StaffMember.class, principal.userId()));
        if (user == null || !user.isActive()) {
            throw new BadCredentialsException("Unknown user");
        }
        if (!tenant.isActive()) {
            throw new BadCredentialsException("Restaurant inactive");
        }
        return MeResponse.from(user, tenant);
    }

    private AuthResponse buildResponse(StaffMember user, Tenant tenant) {
        Instant expiresAt = jwtService.expiresAt();
        String token = jwtService.generate(user, expiresAt);
        return AuthResponse.of(token, expiresAt, MeResponse.from(user, tenant));
    }
}
