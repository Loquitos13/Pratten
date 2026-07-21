package beringela.software.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds a tenant to the request thread.
 *
 * <p>Authenticated endpoints get their tenant from the JWT (pinned later by
 * {@code JwtAuthenticationFilter}, which is authoritative). This filter serves
 * the unauthenticated tenant-scoped surface: the public website API, which must
 * carry an {@code X-Tenant-ID} header. Missing/invalid headers on {@code /public}
 * are rejected to avoid reads or writes under the sentinel tenant.
 *
 * <p>Runs before Spring Security, so its {@code finally} clears the context only
 * after the whole chain (including the JWT filter and controller) has completed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    /** Servlet path (context-path stripped) prefixes that must carry a tenant header. */
    private static final List<String> TENANT_REQUIRED_PREFIXES = List.of("/public");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(TENANT_HEADER);
        try {
            if (StringUtils.hasText(header)) {
                TenantContext.set(UUID.fromString(header.trim()));
            } else if (tenantRequired(request)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing " + TENANT_HEADER + " header");
                return;
            }
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid " + TENANT_HEADER + " header, expected a UUID");
        } finally {
            TenantContext.clear();
        }
    }

    private boolean tenantRequired(HttpServletRequest request) {
        String path = request.getServletPath();
        return TENANT_REQUIRED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
