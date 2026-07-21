package beringela.software.security;

import beringela.software.domain.StaffRole;
import java.util.UUID;

/** Authenticated caller derived from a validated JWT. */
public record AuthPrincipal(UUID userId, UUID tenantId, String name, StaffRole role) {
}
