package beringela.software.dto;

import beringela.software.domain.Reservation;
import beringela.software.domain.ReservationSource;
import beringela.software.domain.ReservationStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    /** Reservation booking submitted from the public website. */
    public record PublicReservationRequest(
            @NotBlank String customerName,
            String customerPhone,
            String customerEmail,
            @Positive int partySize,
            @NotNull @Future Instant reservedAt,
            String notes) {
    }

    /** Reservation created/edited internally by staff. */
    public record ReservationRequest(
            @NotBlank String customerName,
            String customerPhone,
            String customerEmail,
            @Positive int partySize,
            @NotNull Instant reservedAt,
            Integer durationMinutes,
            ReservationSource source,
            UUID tableId,
            String notes) {
    }

    public record ReservationStatusRequest(@NotNull ReservationStatus status, UUID tableId) {
    }

    public record ReservationResponse(
            UUID id,
            String customerName,
            String customerPhone,
            String customerEmail,
            int partySize,
            Instant reservedAt,
            int durationMinutes,
            ReservationStatus status,
            ReservationSource source,
            UUID tableId,
            String tableNumber,
            String notes) {

        public static ReservationResponse from(Reservation r) {
            var table = r.getTable();
            return new ReservationResponse(r.getId(), r.getCustomerName(), r.getCustomerPhone(),
                    r.getCustomerEmail(), r.getPartySize(), r.getReservedAt(), r.getDurationMinutes(),
                    r.getStatus(), r.getSource(),
                    table != null ? table.getId() : null,
                    table != null ? table.getNumber() : null,
                    r.getNotes());
        }
    }
}
