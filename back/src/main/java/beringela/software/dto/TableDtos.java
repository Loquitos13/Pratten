package beringela.software.dto;

import beringela.software.domain.RestaurantTable;
import beringela.software.domain.TableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class TableDtos {

    private TableDtos() {
    }

    public record TableRequest(
            @NotBlank String number,
            String zone,
            @Positive int seats,
            TableStatus status) {
    }

    public record TableStatusRequest(@NotNull TableStatus status) {
    }

    /** Assign (or clear, with a null waiterId) the waiter responsible for a table. */
    public record TableAssignmentRequest(UUID waiterId) {
    }

    public record TableResponse(
            UUID id,
            String number,
            String zone,
            int seats,
            TableStatus status,
            UUID assignedWaiterId,
            String assignedWaiterName) {

        public static TableResponse from(RestaurantTable t) {
            var waiter = t.getAssignedWaiter();
            return new TableResponse(t.getId(), t.getNumber(), t.getZone(), t.getSeats(), t.getStatus(),
                    waiter != null ? waiter.getId() : null,
                    waiter != null ? waiter.getName() : null);
        }
    }
}
