package beringela.software.web;

import beringela.software.domain.OrderStatus;
import beringela.software.dto.OrderDtos.AddItemRequest;
import beringela.software.dto.OrderDtos.CreateOrderRequest;
import beringela.software.dto.OrderDtos.OrderResponse;
import beringela.software.dto.OrderDtos.OrderStatusRequest;
import beringela.software.dto.OrderDtos.PaymentRequest;
import beringela.software.dto.OrderDtos.TipRequest;
import beringela.software.dto.OrderDtos.UpdateItemRequest;
import beringela.software.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(name = "status", required = false) OrderStatus status) {
        var orders = status != null ? orderService.findByStatus(status) : orderService.findActive();
        return orders.stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(orderService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(orderService.create(request));
    }

    @PostMapping("/{id}/items")
    public OrderResponse addItem(@PathVariable UUID id, @Valid @RequestBody AddItemRequest request) {
        return OrderResponse.from(orderService.addItem(id, request));
    }

    @PatchMapping("/{id}/items/{itemId}")
    public OrderResponse updateItem(@PathVariable UUID id, @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        return OrderResponse.from(orderService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public OrderResponse removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return OrderResponse.from(orderService.removeItem(id, itemId));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable UUID id,
            @Valid @RequestBody OrderStatusRequest request) {
        return OrderResponse.from(orderService.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/payments")
    public OrderResponse addPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return OrderResponse.from(orderService.addPayment(id, request));
    }

    @PatchMapping("/{id}/tip")
    public OrderResponse setTip(@PathVariable UUID id, @Valid @RequestBody TipRequest request) {
        return OrderResponse.from(orderService.setTip(id, request.tip()));
    }
}
