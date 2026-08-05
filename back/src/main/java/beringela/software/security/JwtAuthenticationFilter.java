package beringela.software.security;

import beringela.software.common.ApiErrorWriter;
import beringela.software.domain.StaffRole;
import beringela.software.tenant.TenantContext;
import beringela.software.tenant.TenantFilter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida JWT de staff ou superadmin.
 *
 * <p>Superadmins podem actuar num tenant enviando {@code X-Tenant-ID}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SUPERADMIN = "SUPERADMIN";

    private final JwtService jwtService;
    private final ApiErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, ApiErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parse(token);
                PrincipalKind kind = claims.containsKey("kind")
                        ? PrincipalKind.valueOf(claims.get("kind", String.class))
                        : PrincipalKind.STAFF;
                UUID userId = UUID.fromString(claims.getSubject());
                String name = claims.get("name", String.class);

                AuthPrincipal principal;
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (kind == PrincipalKind.PLATFORM) {
                    principal = AuthPrincipal.platform(userId, name);
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + SUPERADMIN));
                    bindTenantForPlatform(request);
                } else if (kind == PrincipalKind.REMOTE) {
                    UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
                    UUID remoteSessionId = UUID.fromString(claims.get("remoteSessionId", String.class));
                    UUID platformAdminId = UUID.fromString(claims.get("platformAdminId", String.class));
                    principal = AuthPrincipal.remote(userId, tenantId, name, remoteSessionId, platformAdminId);
                    authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                    TenantContext.set(tenantId);
                } else {
                    UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
                    StaffRole role = StaffRole.valueOf(claims.get("role", String.class));
                    principal = AuthPrincipal.staff(userId, tenantId, name, role);
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
                    TenantContext.set(tenantId);
                }

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Token inválido ou expirado.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void bindTenantForPlatform(HttpServletRequest request) {
        String header = request.getHeader(TenantFilter.TENANT_HEADER);
        if (StringUtils.hasText(header)) {
            TenantContext.set(UUID.fromString(header.trim()));
        }
    }
}
