package beringela.software.repository;

import beringela.software.domain.OrderItem;
import beringela.software.domain.OrderItemStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByStatusInOrderByCreatedAtAsc(List<OrderItemStatus> statuses);
}
