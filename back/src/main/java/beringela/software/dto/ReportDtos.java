package beringela.software.dto;

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
