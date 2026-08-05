package beringela.software.service;



import beringela.software.common.BusinessException;

import beringela.software.common.NotFoundException;

import beringela.software.domain.MenuItem;

import beringela.software.domain.Order;

import beringela.software.domain.OrderItem;

import beringela.software.domain.OrderItemStatus;

import beringela.software.domain.OrderStatus;

import beringela.software.domain.Payment;

import beringela.software.domain.RestaurantTable;

import beringela.software.domain.StaffMember;

import beringela.software.domain.StaffRole;

import beringela.software.domain.TableStatus;

import beringela.software.dto.OrderDtos.AddItemRequest;

import beringela.software.dto.OrderDtos.CreateOrderRequest;

import beringela.software.dto.OrderDtos.KitchenTicketResponse;

import beringela.software.dto.OrderDtos.OrderResponse;

import beringela.software.dto.OrderDtos.PaymentRequest;

import beringela.software.dto.OrderDtos.UpdateItemRequest;

import beringela.software.dto.TableDtos.TableResponse;

import beringela.software.repository.MenuItemRepository;

import beringela.software.repository.OrderRepository;

import beringela.software.repository.RestaurantTableRepository;

import beringela.software.service.StockService;

import beringela.software.security.AuthPrincipal;

import beringela.software.security.StaffActionGuard;

import beringela.software.sync.SyncEventType;

import beringela.software.sync.SyncService;

import java.math.BigDecimal;

import java.util.List;

import java.util.UUID;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

@Transactional

public class OrderService {



    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(

            OrderStatus.OPEN, OrderStatus.IN_PREPARATION, OrderStatus.READY, OrderStatus.SERVED);



    private final OrderRepository orderRepository;

    private final MenuItemRepository menuItemRepository;

    private final RestaurantTableRepository tableRepository;

    private final SyncService syncService;

    private final StaffActionGuard staffActionGuard;

    private final StockService stockService;



    public OrderService(OrderRepository orderRepository, MenuItemRepository menuItemRepository,

            RestaurantTableRepository tableRepository, SyncService syncService,

            StaffActionGuard staffActionGuard, StockService stockService) {

        this.orderRepository = orderRepository;

        this.menuItemRepository = menuItemRepository;

        this.tableRepository = tableRepository;

        this.syncService = syncService;

        this.staffActionGuard = staffActionGuard;

        this.stockService = stockService;

    }



    @Transactional(readOnly = true)

    public List<Order> findActive() {

        staffActionGuard.assertKitchenOnShift();

        return orderRepository.findByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES);

    }



    @Transactional(readOnly = true)

    public List<Order> findByStatus(OrderStatus status) {

        staffActionGuard.assertKitchenOnShift();

        return orderRepository.findByStatusOrderByCreatedAtAsc(status);

    }



    @Transactional(readOnly = true)

    public Order get(UUID id) {

        staffActionGuard.assertKitchenOnShift();

        return orderRepository.findById(id).orElseThrow(() -> NotFoundException.of("Order", id));

    }



    public Order create(CreateOrderRequest request) {

        AuthPrincipal principal = staffActionGuard.currentPrincipal();

        if (principal.staffRole() == StaffRole.WAITER && request.tableId() == null) {

            throw new BusinessException("Selecciona uma mesa para abrir a conta.");

        }



        StaffMember waiter = staffActionGuard.resolveActingWaiter(request.waiterId());

        if (principal.staffRole() == StaffRole.WAITER) {

            assertSingleActiveTable(waiter.getId(), request.tableId());

        }



        Order order = new Order();

        if (request.tableId() != null) {

            RestaurantTable table = tableRepository.findById(request.tableId())

                    .orElseThrow(() -> NotFoundException.of("RestaurantTable", request.tableId()));

            if (principal.staffRole() == StaffRole.WAITER) {

                staffActionGuard.assertWaiterCanUseTable(table, waiter);

            }

            table.setStatus(TableStatus.OCCUPIED);

            order.setTable(table);

        }

        order.setWaiter(waiter);

        order.setNotes(request.notes());

        Order saved = orderRepository.save(order);

        if (saved.getTable() != null) {

            syncService.publish(SyncEventType.TABLE_UPDATED, TableResponse.from(saved.getTable()));

        }

        return publish(saved);

    }



    public Order addItem(UUID orderId, AddItemRequest request) {

        Order order = getMutable(orderId);

        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())

                .orElseThrow(() -> NotFoundException.of("MenuItem", request.menuItemId()));



        OrderItem item = new OrderItem();

        item.setMenuItem(menuItem);

        item.setName(menuItem.getName());

        item.setUnitPrice(menuItem.getPrice());

        item.setQuantity(request.quantity());

        item.setNotes(request.notes());

        item.setStatus(OrderItemStatus.DRAFT);

        order.addItem(item);

        return publish(orderRepository.save(order));

    }



    /** Envia itens em rascunho para a cozinha (fila KDS). */

    public Order sendToKitchen(UUID orderId) {

        Order order = getMutable(orderId);

        List<OrderItem> draftItems = order.getItems().stream()

                .filter(i -> i.getStatus() == OrderItemStatus.DRAFT)

                .toList();

        if (draftItems.isEmpty()) {

            throw new BusinessException("Não há itens por enviar para a cozinha.");

        }

        draftItems.forEach(i -> {
            stockService.deductForServings(i.getMenuItem().getId(), i.getQuantity());
            i.setStatus(OrderItemStatus.PENDING);
        });

        if (order.getStatus() == OrderStatus.OPEN) {

            order.setStatus(OrderStatus.IN_PREPARATION);

        }

        Order saved = orderRepository.save(order);

        draftItems.forEach(i ->

                syncService.publish(SyncEventType.KITCHEN_UPDATED, KitchenTicketResponse.from(i)));

        return publish(saved);

    }



    public Order updateItem(UUID orderId, UUID itemId, UpdateItemRequest request) {

        Order order = getMutable(orderId);

        OrderItem item = order.getItems().stream()

                .filter(i -> i.getId().equals(itemId))

                .findFirst()

                .orElseThrow(() -> NotFoundException.of("OrderItem", itemId));

        if (item.getStatus() != OrderItemStatus.DRAFT) {

            throw new BusinessException("Só podes alterar itens ainda não enviados à cozinha.");

        }

        if (request.quantity() != null) {

            if (request.quantity() < 1) {

                throw new BusinessException("Quantity must be at least 1");

            }

            item.setQuantity(request.quantity());

        }

        if (request.notes() != null) {

            item.setNotes(request.notes());

        }

        if (request.status() != null) {

            item.setStatus(request.status());

        }

        order.recalculateTotal();

        return publish(orderRepository.save(order));

    }



    public Order removeItem(UUID orderId, UUID itemId) {

        Order order = getMutable(orderId);

        OrderItem item = order.getItems().stream()

                .filter(i -> i.getId().equals(itemId))

                .findFirst()

                .orElseThrow(() -> NotFoundException.of("OrderItem", itemId));

        if (item.getStatus() != OrderItemStatus.DRAFT) {

            throw new BusinessException("Só podes remover itens ainda não enviados à cozinha.");

        }

        order.getItems().remove(item);

        order.recalculateTotal();

        return publish(orderRepository.save(order));

    }



    public Order changeStatus(UUID orderId, OrderStatus status) {

        Order order = getMutable(orderId);

        if (order.getStatus() == OrderStatus.PAID) {

            throw new BusinessException("A paid order cannot change status");

        }

        order.setStatus(status);

        if (status == OrderStatus.PAID || status == OrderStatus.CANCELLED) {

            freeTable(order);

        }

        return publish(orderRepository.save(order));

    }



    public Order addPayment(UUID orderId, PaymentRequest request) {

        Order order = getMutable(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {

            throw new BusinessException("Cannot pay a cancelled order");

        }

        if (request.tip() != null && request.tip().signum() > 0) {

            order.setTip(order.getTip().add(request.tip()));

        }

        Payment payment = new Payment();

        payment.setOrder(order);

        payment.setMethod(request.method());

        payment.setAmount(request.amount());

        order.getPayments().add(payment);



        if (order.getBalance().signum() <= 0) {

            order.setStatus(OrderStatus.PAID);

            freeTable(order);

        }

        return publish(orderRepository.save(order));

    }



    /** Sets the tip (gorjeta) on an order that is not yet closed. */

    public Order setTip(UUID orderId, BigDecimal tip) {

        Order order = getMutable(orderId);

        order.setTip(tip);

        return publish(orderRepository.save(order));

    }



    private void assertSingleActiveTable(UUID waiterId, UUID tableId) {

        if (orderRepository.existsByWaiterIdAndStatusInAndTableIdNot(

                waiterId, ACTIVE_STATUSES, tableId)) {

            throw new BusinessException(

                    "Já tens uma mesa activa. Envia ou fecha o pedido antes de mudar de mesa.");

        }

    }



    private Order publish(Order order) {

        syncService.publish(SyncEventType.ORDER_UPDATED, OrderResponse.from(order));

        return order;

    }



    private Order getMutable(UUID orderId) {

        Order order = get(orderId);

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {

            throw new BusinessException("Cannot modify a " + order.getStatus() + " order");

        }

        staffActionGuard.assertCanModifyOrder(order);

        return order;

    }



    private void freeTable(Order order) {

        RestaurantTable table = order.getTable();

        if (table != null) {

            table.setStatus(TableStatus.FREE);

            syncService.publish(SyncEventType.TABLE_UPDATED, TableResponse.from(table));

        }

    }

}


