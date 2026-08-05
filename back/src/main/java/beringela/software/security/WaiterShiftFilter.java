package beringela.software.security;

import beringela.software.domain.StaffRole;
import beringela.software.repository.WorkShiftRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Empregado de mesa sem turno activo só acede a clock in e consulta do próprio estado.
 */
@Component
public class WaiterShiftFilter extends OncePerRequestFilter {

    private static final Set<String> PATHS_WITHOUT_SHIFT = Set.of(
            "/auth/me",
            "/shifts/me",
            "/shifts/clock-in");

    private final WorkShiftRepository shiftRepository;

    public WaiterShiftFilter(WorkShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal
                && principal.staffRole() == StaffRole.WAITER
                && !isAllowedWithoutShift(request)) {
            boolean onShift = shiftRepository
                    .findByStaffMemberIdAndClockOutAtIsNull(principal.userId())
                    .isPresent();
            if (!onShift) {
                response.sendError(422, "Tens de fazer clock in ao entrar no turno.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWithoutShift(HttpServletRequest request) {
        return PATHS_WITHOUT_SHIFT.contains(normalizePath(request));
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path != null && !path.isBlank()) {
            return path;
        }
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isBlank() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }
}
