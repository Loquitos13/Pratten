package beringela.software.service;

import beringela.software.common.PrattenJson;
import beringela.software.domain.PlatformNotification;
import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import beringela.software.platform.PlatformNotificationBus;
import beringela.software.platform.PlatformNotificationLocalRegistry;
import beringela.software.repository.PlatformNotificationRepository;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PlatformNotificationStreamService {

    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final PlatformNotificationLocalRegistry registry;
    private final PlatformNotificationBus notificationBus;
    private final PlatformNotificationRepository notificationRepository;

    public PlatformNotificationStreamService(PlatformNotificationLocalRegistry registry,
            PlatformNotificationBus notificationBus,
            PlatformNotificationRepository notificationRepository) {
        this.registry = registry;
        this.notificationBus = notificationBus;
        this.notificationRepository = notificationRepository;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        registry.register(emitter);

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            long unread = notificationRepository.countByReadFalse();
            emitter.send(SseEmitter.event().name("unread_count").data(unread));
        } catch (IOException ex) {
            // ignored
        }
        return emitter;
    }

    public void broadcast(PlatformNotification notification) {
        PlatformNotificationResponse payload = PlatformNotificationResponse.from(notification);
        notificationBus.broadcast(PrattenJson.write(payload));
    }
}
