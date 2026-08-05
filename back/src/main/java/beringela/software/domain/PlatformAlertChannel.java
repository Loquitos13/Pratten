package beringela.software.domain;

import beringela.software.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Destino externo para alertas (Slack, PagerDuty, email de plantão, etc.). */
@Entity
@Table(name = "platform_alert_channels")
@Getter
@Setter
public class PlatformAlertChannel extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private AlertChannelType channelType;

    /** URL webhook ou endereço email. */
    @Column(nullable = false, length = 500)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, length = 20)
    private PlatformNotificationSeverity minSeverity = PlatformNotificationSeverity.WARNING;

    /** Tipos de evento separados por vírgula; vazio = todos. */
    @Column(name = "event_types", length = 500)
    private String eventTypes;

    @Column(nullable = false)
    private boolean active = true;
}
