package beringela.software.dto;

import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String restaurantName,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{2,50}",
                    message = "slug must be lowercase letters, digits or hyphens") String slug,
            @NotBlank String managerName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(
            @NotBlank String slug,
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record MeResponse(
            UUID userId,
            String name,
            String email,
            StaffRole role,
            UUID tenantId,
            String tenantName,
            String tenantSlug) {

        public static MeResponse from(StaffMember user, Tenant tenant) {
            return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                    tenant.getId(), tenant.getName(), tenant.getSlug());
        }
    }

    public record AuthResponse(
            String token,
            String tokenType,
            Instant expiresAt,
            MeResponse user) {

        public static AuthResponse of(String token, Instant expiresAt, MeResponse user) {
            return new AuthResponse(token, "Bearer", expiresAt, user);
        }
    }
}
