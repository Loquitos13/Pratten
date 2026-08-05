-- Sessões remotas de suporte, saúde operacional dos tenants e notificações platform.

CREATE TABLE remote_sessions (
    id UUID PRIMARY KEY,
    platform_admin_id UUID NOT NULL REFERENCES platform_admins (id),
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    acting_staff_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_remote_sessions_tenant ON remote_sessions (tenant_id);
CREATE INDEX idx_remote_sessions_admin ON remote_sessions (platform_admin_id);

CREATE TABLE tenant_health_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants (id),
    status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    active_sync_connections INT NOT NULL DEFAULT 0,
    avg_latency_ms BIGINT NOT NULL DEFAULT 0,
    pending_sync_events BIGINT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    last_checked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE TABLE platform_notifications (
    id UUID PRIMARY KEY,
    tenant_id UUID REFERENCES tenants (id),
    severity VARCHAR(20) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_platform_notifications_unread ON platform_notifications (read_flag, created_at DESC);
CREATE INDEX idx_platform_notifications_tenant ON platform_notifications (tenant_id);
