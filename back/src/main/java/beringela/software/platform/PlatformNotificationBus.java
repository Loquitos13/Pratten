package beringela.software.platform;

/** Publica notificações platform para SSE (local ou Redis). */
public interface PlatformNotificationBus {

    void broadcast(String notificationJson);
}
