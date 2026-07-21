package beringela.software.repository;

import beringela.software.domain.RestaurantTable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    List<RestaurantTable> findAllByOrderByZoneAscNumberAsc();

    List<RestaurantTable> findByAssignedWaiterIdOrderByNumberAsc(UUID waiterId);
}
