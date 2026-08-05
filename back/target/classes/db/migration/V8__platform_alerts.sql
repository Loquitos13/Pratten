-- Canais de alerta externo (webhook, email) e histórico de entregas.

CREATE TABLE platform_alert_channels (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    target VARCHAR(500) NOT NULL,
    min_severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    event_types VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE TABLE platform_alert_deliveries (
    id UUID PRIMARY KEY,
    channel_id UUID NOT NULL REFERENCES platform_alert_channels (id),
    notification_id UUID REFERENCES platform_notifications (id),
    event_type VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_code INT,
    error_detail VARCHAR(500),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_alert_deliveries_channel ON platform_alert_deliveries (channel_id, attempted_at DESC);
