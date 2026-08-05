package beringela.software.domain;

import beringela.software.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_alert_deliveries")
@Getter
@Setter
public class PlatformAlertDelivery extends BaseEntity {

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertDeliveryStatus status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "error_detail", length = 500)
    private String errorDetail;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
