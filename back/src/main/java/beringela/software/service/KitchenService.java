package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.OrderItem;
import beringela.software.domain.OrderItemStatus;
import beringela.software.dto.OrderDtos.KitchenTicketResponse;
import beringela.software.dto.OrderDtos.OrderResponse;
import beringela.software.repository.OrderItemRepository;
import beringela.software.sync.SyncEventType;
import beringela.software.sync.SyncService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Backs the Kitchen Display System (KDS): the live queue of items to prepare. */
@Service
@Transactional
public class KitchenService {

    private static final List<OrderItemStatus> QUEUE_STATUSES =
            List.of(OrderItemStatus.PENDING, OrderItemStatus.IN_PREPARATION);

    private final OrderItemRepository orderItemRepository;
    private final SyncService syncService;

    public KitchenService(OrderItemRepository orderItemRepository, SyncService syncService) {
        this.orderItemRepository = orderItemRepository;
        this.syncService = syncService;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> queue() {
        return orderItemRepository.findByStatusInOrderByCreatedAtAsc(QUEUE_STATUSES);
    }

    public OrderItem updateStatus(UUID itemId, OrderItemStatus status) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> NotFoundException.of("OrderItem", itemId));
        item.setStatus(status);
        OrderItem saved = orderItemRepository.save(item);
        syncService.publish(SyncEventType.KITCHEN_UPDATED, KitchenTicketResponse.from(saved));
        if (saved.getOrder() != null) {
            syncService.publish(SyncEventType.ORDER_UPDATED, OrderResponse.from(saved.getOrder()));
        }
        return saved;
    }
}
