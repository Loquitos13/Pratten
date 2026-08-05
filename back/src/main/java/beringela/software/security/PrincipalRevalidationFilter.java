package beringela.software.security;

import beringela.software.common.ApiErrorWriter;
import beringela.software.domain.PlatformAdmin;
import beringela.software.domain.RemoteSession;
import beringela.software.domain.StaffMember;
import beringela.software.domain.Tenant;
import beringela.software.repository.PlatformAdminRepository;
import beringela.software.repository.RemoteSessionRepository;
import beringela.software.repository.TenantRepository;
import java.time.Instant;
import beringela.software.tenant.TenantContext;
import beringela.software.tenant.TenantSessionExecutor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revalida em cada pedido que o tenant, staff ou superadmin ainda está activo.
 * Tokens válidos de contas desactivadas passam a ser rejeitados.
 */
@Component
public class PrincipalRevalidationFilter extends OncePerRequestFilter {

    private final ApiErrorWriter errorWriter;
    private final TenantRepository tenantRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final RemoteSessionRepository remoteSessionRepository;
    private final TenantSessionExecutor tenantSessions;

    public PrincipalRevalidationFilter(ApiErrorWriter errorWriter,
            TenantRepository tenantRepository,
            PlatformAdminRepository platformAdminRepository,
            RemoteSessionRepository remoteSessionRepository,
            TenantSessionExecutor tenantSessions) {
        this.errorWriter = errorWriter;
        this.tenantRepository = tenantRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.remoteSessionRepository = remoteSessionRepository;
        this.tenantSessions = tenantSessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            if (!isStillValid(principal)) {
                SecurityContextHolder.clearContext();
                errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                        "Conta ou restaurante inactivo. Inicia sessão novamente.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isStillValid(AuthPrincipal principal) {
        if (principal.isPlatformAdmin()) {
            return platformAdminRepository.findById(principal.userId())
                    .filter(PlatformAdmin::isActive)
                    .isPresent()
                    && isTenantContextActive();
        }
        if (principal.isRemoteSupport()) {
            return isRemoteSessionValid(principal);
        }
        UUID tenantId = principal.tenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isActive()) {
            return false;
        }
        StaffMember staff = tenantSessions.inTenant(tenantId,
                session -> session.find(StaffMember.class, principal.userId()));
        return staff != null && staff.isActive();
    }

    private boolean isRemoteSessionValid(AuthPrincipal principal) {
        UUID tenantId = principal.tenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || !tenant.isActive()) {
            return false;
        }
        RemoteSession session = remoteSessionRepository.findById(principal.remoteSessionId())
                .orElse(null);
        if (session == null || !session.isActive() || session.getEndedAt() != null
                || session.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        return platformAdminRepository.findById(principal.platformAdminId())
                .filter(PlatformAdmin::isActive)
                .isPresent();
    }

    private boolean isTenantContextActive() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            return true;
        }
        return tenantRepository.findById(tenantId)
                .map(Tenant::isActive)
                .orElse(false);
    }
}
