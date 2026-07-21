package beringela.software.service;

import beringela.software.domain.Order;
import beringela.software.domain.OrderItem;
import beringela.software.domain.OrderItemStatus;
import beringela.software.domain.OrderStatus;
import beringela.software.dto.ReportDtos.SalesReport;
import beringela.software.dto.ReportDtos.WaiterReport;
import beringela.software.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manager reporting: what each waiter earned, tables served and tips collected. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final UUID UNASSIGNED = new UUID(0L, 0L);

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public SalesReport waiterSales(Instant from, Instant to) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(from, to).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        Map<UUID, Accumulator> byWaiter = new LinkedHashMap<>();
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalTips = BigDecimal.ZERO;

        for (Order order : orders) {
            UUID waiterId = order.getWaiter() != null ? order.getWaiter().getId() : UNASSIGNED;
            String waiterName = order.getWaiter() != null ? order.getWaiter().getName() : "Sem empregado";
            Accumulator acc = byWaiter.computeIfAbsent(waiterId, id -> new Accumulator(id, waiterName));
            acc.add(order);
            totalSales = totalSales.add(order.getTotal());
            totalTips = totalTips.add(order.getTip());
        }

        List<WaiterReport> waiters = new ArrayList<>();
        for (Accumulator acc : byWaiter.values()) {
            waiters.add(acc.toReport());
        }
        waiters.sort(Comparator.comparing(WaiterReport::grandTotal).reversed());

        return new SalesReport(from, to, orders.size(), totalSales, totalTips,
                totalSales.add(totalTips), waiters);
    }

    private static final class Accumulator {
        private final UUID waiterId;
        private final String waiterName;
        private final java.util.Set<UUID> tables = new java.util.HashSet<>();
        private long orders;
        private long items;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal tips = BigDecimal.ZERO;

        Accumulator(UUID waiterId, String waiterName) {
            this.waiterId = waiterId;
            this.waiterName = waiterName;
        }

        void add(Order order) {
            orders++;
            sales = sales.add(order.getTotal());
            tips = tips.add(order.getTip());
            if (order.getTable() != null) {
                tables.add(order.getTable().getId());
            }
            items += order.getItems().stream()
                    .filter(i -> i.getStatus() != OrderItemStatus.CANCELLED)
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
        }

        WaiterReport toReport() {
            return new WaiterReport(waiterId, waiterName, tables.size(), orders, items,
                    sales, tips, sales.add(tips));
        }
    }
}
