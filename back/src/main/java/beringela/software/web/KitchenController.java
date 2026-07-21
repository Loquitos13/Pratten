package beringela.software.web;

import beringela.software.dto.OrderDtos.ItemStatusRequest;
import beringela.software.dto.OrderDtos.KitchenTicketResponse;
import beringela.software.dto.OrderDtos.OrderItemResponse;
import beringela.software.service.KitchenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kitchen Display System endpoints. */
@RestController
@RequestMapping("/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/queue")
    public List<KitchenTicketResponse> queue() {
        return kitchenService.queue().stream().map(KitchenTicketResponse::from).toList();
    }

    @PatchMapping("/items/{itemId}/status")
    public OrderItemResponse updateStatus(@PathVariable UUID itemId,
            @Valid @RequestBody ItemStatusRequest request) {
        return OrderItemResponse.from(kitchenService.updateStatus(itemId, request.status()));
    }
}
