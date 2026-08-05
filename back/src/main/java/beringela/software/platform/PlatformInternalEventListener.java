package beringela.software.platform;

import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.security.LoginLockoutEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Converte eventos internos em eventos platform assíncronos. */
@Component
public class PlatformInternalEventListener {

    private final PlatformEventPublisher eventPublisher;

    public PlatformInternalEventListener(PlatformEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onLoginLockout(LoginLockoutEvent event) {
        eventPublisher.publish(PlatformEvent.of(
                PlatformEventType.LOGIN_LOCKOUT_SPIKE,
                null,
                null,
                PlatformNotificationSeverity.WARNING,
                "Conta bloqueada por tentativas",
                "Conta " + maskKey(event.accountKey()) + " bloqueada (" + event.scope() + ")"));
    }

    private String maskKey(String key) {
        int colon = key.indexOf(':');
        if (colon < 0 || colon >= key.length() - 1) {
            return "***";
        }
        String prefix = key.substring(0, colon + 1);
        String value = key.substring(colon + 1);
        if (value.length() <= 3) {
            return prefix + "***";
        }
        return prefix + value.substring(0, 2) + "***";
    }
}
