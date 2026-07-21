package beringela.software.dto;

import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record StaffRequest(
            @NotBlank String name,
            @Email String email,
            String pin,
            @NotNull StaffRole role,
            Boolean active,
            String password) {
    }

    public record StaffResponse(
            UUID id,
            String name,
            String email,
            StaffRole role,
            boolean active) {

        public static StaffResponse from(StaffMember s) {
            return new StaffResponse(s.getId(), s.getName(), s.getEmail(), s.getRole(), s.isActive());
        }
    }
}
