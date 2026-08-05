package beringela.software.web;

import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import beringela.software.service.PlatformNotificationService;
import beringela.software.service.PlatformNotificationStreamService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Notificações in-app e stream SSE para superadmins. */
@RestController
@RequestMapping("/platform/notifications")
public class PlatformNotificationController {

    private final PlatformNotificationService notificationService;
    private final PlatformNotificationStreamService streamService;

    public PlatformNotificationController(PlatformNotificationService notificationService,
            PlatformNotificationStreamService streamService) {
        this.notificationService = notificationService;
        this.streamService = streamService;
    }

    @GetMapping
    public List<PlatformNotificationResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.listRecent(unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount());
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
    }

    @PatchMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return streamService.subscribe();
    }
}
