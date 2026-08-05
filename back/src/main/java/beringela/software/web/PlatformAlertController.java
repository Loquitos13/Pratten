package beringela.software.web;

import beringela.software.dto.PlatformDtos.AlertChannelRequest;
import beringela.software.dto.PlatformDtos.AlertChannelResponse;
import beringela.software.dto.PlatformDtos.AlertDeliveryResponse;
import beringela.software.service.PlatformAlertChannelService;
import beringela.software.service.PlatformAlertDispatcher;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Canais de alerta externo (webhook, email). */
@RestController
@RequestMapping("/platform/alerts")
public class PlatformAlertController {

    private final PlatformAlertChannelService channelService;
    private final PlatformAlertDispatcher alertDispatcher;

    public PlatformAlertController(PlatformAlertChannelService channelService,
            PlatformAlertDispatcher alertDispatcher) {
        this.channelService = channelService;
        this.alertDispatcher = alertDispatcher;
    }

    @GetMapping("/channels")
    public List<AlertChannelResponse> listChannels() {
        return channelService.list();
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertChannelResponse createChannel(@Valid @RequestBody AlertChannelRequest request) {
        return channelService.create(request);
    }

    @PutMapping("/channels/{id}")
    public AlertChannelResponse updateChannel(@PathVariable UUID id,
            @Valid @RequestBody AlertChannelRequest request) {
        return channelService.update(id, request);
    }

    @DeleteMapping("/channels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(@PathVariable UUID id) {
        channelService.delete(id);
    }

    @GetMapping("/channels/{id}/deliveries")
    public List<AlertDeliveryResponse> deliveries(@PathVariable UUID id) {
        return alertDispatcher.recentDeliveries(id);
    }
}
