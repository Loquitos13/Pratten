package beringela.software.platform;

/** Mensagem Pub/Sub para notificações platform entre instâncias. */
public record PlatformNotificationMessage(String notificationJson) {
}
