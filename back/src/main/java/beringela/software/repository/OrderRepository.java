package beringela.software.repository;

import beringela.software.domain.Order;
import beringela.software.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);

    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findByCreatedAtBetween(Instant start, Instant end);
}
