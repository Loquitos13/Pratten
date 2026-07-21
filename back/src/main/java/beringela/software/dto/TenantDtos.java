package beringela.software.dto;

import beringela.software.domain.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public final class TenantDtos {

    private TenantDtos() {
    }

    public record CreateTenantRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{2,50}",
                    message = "slug must be lowercase letters, digits or hyphens") String slug,
            String vatNumber,
            String address,
            String currency) {
    }

    public record TenantResponse(
            UUID id,
            String name,
            String slug,
            String vatNumber,
            String address,
            String currency,
            boolean active) {

        public static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getName(), t.getSlug(),
                    t.getVatNumber(), t.getAddress(), t.getCurrency(), t.isActive());
        }
    }
}
