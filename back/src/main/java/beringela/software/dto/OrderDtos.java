package beringela.software.dto;

import beringela.software.domain.Order;
import beringela.software.domain.OrderItem;
import beringela.software.domain.OrderItemStatus;
import beringela.software.domain.OrderStatus;
import beringela.software.domain.Payment;
import beringela.software.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    // ---- Requests ----

    public record CreateOrderRequest(UUID tableId, UUID waiterId, String notes) {
    }

    public record AddItemRequest(
            @NotNull UUID menuItemId,
            @Positive int quantity,
            String notes) {
    }

    public record UpdateItemRequest(Integer quantity, String notes, OrderItemStatus status) {
    }

    public record OrderStatusRequest(@NotNull OrderStatus status) {
    }

    public record ItemStatusRequest(@NotNull OrderItemStatus status) {
    }

    public record PaymentRequest(
            @NotNull PaymentMethod method,
            @NotNull @Positive BigDecimal amount,
            /** Optional tip captured together with this payment. */
            BigDecimal tip) {
    }

    public record TipRequest(@NotNull @PositiveOrZero BigDecimal tip) {
    }

    // ---- Responses ----

    public record OrderItemResponse(
            UUID id,
            UUID menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String notes,
            OrderItemStatus status) {

        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getId(),
                    i.getMenuItem() != null ? i.getMenuItem().getId() : null,
                    i.getName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal(),
                    i.getNotes(), i.getStatus());
        }
    }

    public record PaymentResponse(UUID id, PaymentMethod method, BigDecimal amount, Instant createdAt) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getMethod(), p.getAmount(), p.getCreatedAt());
        }
    }

    public record OrderResponse(
            UUID id,
            UUID tableId,
            String tableNumber,
            UUID waiterId,
            String waiterName,
            OrderStatus status,
            String notes,
            BigDecimal total,
            BigDecimal tip,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal balance,
            List<OrderItemResponse> items,
            List<PaymentResponse> payments,
            Instant createdAt) {

        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(),
                    o.getTable() != null ? o.getTable().getId() : null,
                    o.getTable() != null ? o.getTable().getNumber() : null,
                    o.getWaiter() != null ? o.getWaiter().getId() : null,
                    o.getWaiter() != null ? o.getWaiter().getName() : null,
                    o.getStatus(), o.getNotes(), o.getTotal(), o.getTip(), o.getGrandTotal(),
                    o.getPaidAmount(), o.getBalance(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(),
                    o.getPayments().stream().map(PaymentResponse::from).toList(),
                    o.getCreatedAt());
        }
    }

    /** Flattened kitchen queue entry (an order item with its order/table context). */
    public record KitchenTicketResponse(
            UUID orderItemId,
            UUID orderId,
            UUID tableId,
            String tableNumber,
            String tableZone,
            String name,
            int quantity,
            String notes,
            OrderItemStatus status,
            Instant createdAt) {

        public static KitchenTicketResponse from(OrderItem i) {
            var order = i.getOrder();
            var table = order != null ? order.getTable() : null;
            return new KitchenTicketResponse(
                    i.getId(),
                    order != null ? order.getId() : null,
                    table != null ? table.getId() : null,
                    table != null ? table.getNumber() : null,
                    table != null ? table.getZone() : null,
                    i.getName(), i.getQuantity(), i.getNotes(),
                    i.getStatus(), i.getCreatedAt());
        }
    }
}
