-- Tips (gorjetas), waiter->table assignments and website reservations.

ALTER TABLE orders
    ADD COLUMN tip NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE restaurant_tables
    ADD COLUMN assigned_waiter_id UUID REFERENCES staff_members (id);

CREATE INDEX idx_tables_assigned_waiter ON restaurant_tables (assigned_waiter_id);

CREATE TABLE reservations (
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    tenant_id        UUID         NOT NULL,
    customer_name    VARCHAR(255) NOT NULL,
    customer_phone   VARCHAR(50),
    customer_email   VARCHAR(255),
    party_size       INTEGER      NOT NULL DEFAULT 2,
    reserved_at      TIMESTAMPTZ  NOT NULL,
    duration_minutes INTEGER      NOT NULL DEFAULT 120,
    status           VARCHAR(30)  NOT NULL,
    source           VARCHAR(30)  NOT NULL,
    notes            VARCHAR(500),
    table_id         UUID REFERENCES restaurant_tables (id)
);

CREATE INDEX idx_reservations_tenant_status ON reservations (tenant_id, status);
CREATE INDEX idx_reservations_reserved_at ON reservations (tenant_id, reserved_at);
