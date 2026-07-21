package beringela.software.repository;

import beringela.software.domain.Reservation;
import beringela.software.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByStatusOrderByReservedAtAsc(ReservationStatus status);

    List<Reservation> findByReservedAtBetweenOrderByReservedAtAsc(Instant start, Instant end);

    List<Reservation> findAllByOrderByReservedAtAsc();
}
