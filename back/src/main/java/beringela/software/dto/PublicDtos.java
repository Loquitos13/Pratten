package beringela.software.dto;

import beringela.software.domain.RestaurantTable;
import beringela.software.domain.TableStatus;
import beringela.software.dto.TableDtos.TableResponse;
import java.util.List;

public final class PublicDtos {

    private PublicDtos() {
    }

    /** Live table availability the public website mirrors from the POS/mobile. */
    public record AvailabilityResponse(
            int total,
            int free,
            int occupied,
            int reserved,
            List<TableResponse> tables) {

        public static AvailabilityResponse from(List<RestaurantTable> tables) {
            int free = (int) tables.stream().filter(t -> t.getStatus() == TableStatus.FREE).count();
            int occupied = (int) tables.stream().filter(t -> t.getStatus() == TableStatus.OCCUPIED).count();
            int reserved = (int) tables.stream().filter(t -> t.getStatus() == TableStatus.RESERVED).count();
            return new AvailabilityResponse(tables.size(), free, occupied, reserved,
                    tables.stream().map(TableResponse::from).toList());
        }
    }
}
