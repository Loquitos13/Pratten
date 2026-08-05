package beringela.software.dto;

import beringela.software.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {

    private ReportDtos() {
    }

    /** Per-waiter performance the manager needs: sales, tables, tips. */
    public record WaiterReport(
            UUID waiterId,
            String waiterName,
            long tablesServed,
            long ordersCount,
            long itemsSold,
            BigDecimal salesTotal,
            BigDecimal tipsTotal,
            BigDecimal grandTotal) {
    }

    /** Detalhe de um pedido/fatura atribuído a um empregado. */
    public record StaffOrderActivity(
            UUID orderId,
            String tableNumber,
            OrderStatus status,
            BigDecimal total,
            BigDecimal tip,
            BigDecimal paidAmount,
            long itemsCount,
            Instant createdAt) {
    }

    public record StaffActivityReport(
            UUID staffId,
            String staffName,
            Instant from,
            Instant to,
            long ordersCount,
            BigDecimal salesTotal,
            BigDecimal tipsTotal,
            List<StaffOrderActivity> orders) {
    }

    public record SalesReport(
            Instant from,
            Instant to,
            long ordersCount,
            BigDecimal salesTotal,
            BigDecimal tipsTotal,
            BigDecimal grandTotal,
            List<WaiterReport> waiters) {
    }
}
