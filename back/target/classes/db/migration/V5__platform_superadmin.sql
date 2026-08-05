-- Superadmin da plataforma (fora do multi-tenant) e auditoria de acções de suporte.
CREATE TABLE platform_admins (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE TABLE platform_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES platform_admins (id),
    tenant_id UUID REFERENCES tenants (id),
    action VARCHAR(100) NOT NULL,
    detail VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

ALTER TABLE tenants ADD COLUMN support_notes VARCHAR(2000);
