package beringela.software.platform;

import beringela.software.common.PrattenJson;
import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import beringela.software.repository.PlatformNotificationRepository;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class PlatformNotificationLocalRegistry {

    private static final Logger log = LoggerFactory.getLogger(PlatformNotificationLocalRegistry.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final PlatformNotificationRepository notificationRepository;

    public PlatformNotificationLocalRegistry(PlatformNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void register(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
    }

    public void pushNotificationJson(String notificationJson) {
        try {
            PlatformNotificationResponse payload =
                    PrattenJson.read(notificationJson, PlatformNotificationResponse.class);
            fanOut(payload);
        } catch (RuntimeException ex) {
            log.warn("Invalid platform notification payload: {}", ex.getMessage());
        }
    }

    public void fanOut(PlatformNotificationResponse payload) {
        long unread = notificationRepository.countByReadFalse();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
                emitter.send(SseEmitter.event().name("unread_count").data(unread));
            } catch (IOException | IllegalStateException ex) {
                emitters.remove(emitter);
            }
        }
        log.debug("Notification pushed to {} local platform SSE clients", emitters.size());
    }
}
