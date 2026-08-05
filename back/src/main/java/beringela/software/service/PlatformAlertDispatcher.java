package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.AlertChannelType;
import beringela.software.domain.AlertDeliveryStatus;
import beringela.software.domain.PlatformAlertChannel;
import beringela.software.domain.PlatformAlertDelivery;
import beringela.software.domain.PlatformNotification;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.dto.PlatformDtos.AlertDeliveryResponse;
import beringela.software.platform.PlatformEvent;
import beringela.software.repository.PlatformAlertChannelRepository;
import beringela.software.repository.PlatformAlertDeliveryRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Despacha alertas para webhooks e email conforme canais configurados. */
@Service
public class PlatformAlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PlatformAlertDispatcher.class);

    private final PlatformAlertChannelRepository channelRepository;
    private final PlatformAlertDeliveryRepository deliveryRepository;
    private final RestClient restClient;
    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final boolean mailEnabled;

    public PlatformAlertDispatcher(
            PlatformAlertChannelRepository channelRepository,
            PlatformAlertDeliveryRepository deliveryRepository,
            org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${pratten.platform.alerts.mail.from:alerts@pratten.pt}") String mailFrom,
            @Value("${pratten.platform.alerts.mail.enabled:false}") boolean mailEnabled) {
        this.channelRepository = channelRepository;
        this.deliveryRepository = deliveryRepository;
        this.restClient = RestClient.create();
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailFrom = mailFrom;
        this.mailEnabled = mailEnabled && mailSender != null;
    }

    public void dispatch(PlatformEvent event, PlatformNotification notification) {
        for (PlatformAlertChannel channel : channelRepository.findByActiveTrueOrderByNameAsc()) {
            if (!matches(channel, event)) {
                recordDelivery(channel.getId(), notification.getId(), event.type().name(),
                        AlertDeliveryStatus.SKIPPED, null, "Filtrado por severidade/tipo");
                continue;
            }
            try {
                int code = switch (channel.getChannelType()) {
                    case WEBHOOK -> sendWebhook(channel, event, notification);
                    case EMAIL -> sendEmail(channel, event, notification);
                };
                recordDelivery(channel.getId(), notification.getId(), event.type().name(),
                        AlertDeliveryStatus.SENT, code, null);
            } catch (Exception ex) {
                log.warn("Alert delivery failed channel={}: {}", channel.getName(), ex.getMessage());
                recordDelivery(channel.getId(), notification.getId(), event.type().name(),
                        AlertDeliveryStatus.FAILED, null, truncate(ex.getMessage()));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AlertDeliveryResponse> recentDeliveries(UUID channelId) {
        return deliveryRepository.findTop50ByChannelIdOrderByAttemptedAtDesc(channelId).stream()
                .map(AlertDeliveryResponse::from)
                .toList();
    }

    private boolean matches(PlatformAlertChannel channel, PlatformEvent event) {
        if (severityRank(event.severity()) < severityRank(channel.getMinSeverity())) {
            return false;
        }
        if (!StringUtils.hasText(channel.getEventTypes())) {
            return true;
        }
        return Arrays.stream(channel.getEventTypes().split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(t -> t.equals(event.type().name()));
    }

    private int sendWebhook(PlatformAlertChannel channel, PlatformEvent event,
            PlatformNotification notification) {
        Map<String, Object> body = Map.of(
                "eventType", event.type().name(),
                "severity", event.severity().name(),
                "title", notification.getTitle(),
                "message", notification.getMessage(),
                "tenantId", event.tenantId() != null ? event.tenantId().toString() : "",
                "occurredAt", event.occurredAt().toString());
        return restClient.post()
                .uri(channel.getTarget())
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value();
    }

    private int sendEmail(PlatformAlertChannel channel, PlatformEvent event,
            PlatformNotification notification) {
        if (!mailEnabled) {
            throw new BusinessException("SMTP não configurado - canal email indisponível.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(channel.getTarget());
        message.setSubject("[Pratten " + event.severity().name() + "] " + notification.getTitle());
        message.setText(notification.getMessage() + "\n\nEvento: " + event.type().name()
                + "\nOcorrido: " + event.occurredAt());
        mailSender.send(message);
        return 200;
    }

    private void recordDelivery(UUID channelId, UUID notificationId, String eventType,
            AlertDeliveryStatus status, Integer responseCode, String error) {
        PlatformAlertDelivery delivery = new PlatformAlertDelivery();
        delivery.setChannelId(channelId);
        delivery.setNotificationId(notificationId);
        delivery.setEventType(eventType);
        delivery.setStatus(status);
        delivery.setResponseCode(responseCode);
        delivery.setErrorDetail(error);
        delivery.setAttemptedAt(Instant.now());
        deliveryRepository.save(delivery);
    }

    private int severityRank(PlatformNotificationSeverity severity) {
        return switch (severity) {
            case INFO -> 1;
            case WARNING -> 2;
            case CRITICAL -> 3;
        };
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
