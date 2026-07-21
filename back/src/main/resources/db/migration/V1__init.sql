-- Pratten initial schema (PostgreSQL). Multi-tenant, shared-schema with a
-- tenant_id discriminator managed by Hibernate @TenantId.

CREATE TABLE tenants (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(50)  NOT NULL UNIQUE,
    vat_number  VARCHAR(50),
    address     VARCHAR(255),
    currency    VARCHAR(10)  NOT NULL DEFAULT 'EUR',
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE staff_members (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    pin         VARCHAR(255),
    role        VARCHAR(30)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0,
    tenant_id     UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT         NOT NULL DEFAULT 0,
    tenant_id   UUID           NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    barcode     VARCHAR(64),
    category_id UUID REFERENCES categories (id),
    unit        VARCHAR(20)    NOT NULL,
    quantity    NUMERIC(12, 3) NOT NULL DEFAULT 0,
    min_stock   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    price       NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE TABLE menu_items (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT         NOT NULL DEFAULT 0,
    tenant_id   UUID           NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description VARCHAR(1000),
    category_id UUID REFERENCES categories (id),
    price       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    available   BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE restaurant_tables (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,
    tenant_id   UUID         NOT NULL,
    number      VARCHAR(50)  NOT NULL,
    zone        VARCHAR(100),
    seats       INTEGER      NOT NULL DEFAULT 2,
    status      VARCHAR(30)  NOT NULL,
    CONSTRAINT uq_table_number_per_tenant UNIQUE (tenant_id, number)
);

CREATE TABLE orders (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT         NOT NULL DEFAULT 0,
    tenant_id   UUID           NOT NULL,
    table_id    UUID REFERENCES restaurant_tables (id),
    waiter_id   UUID REFERENCES staff_members (id),
    status      VARCHAR(30)    NOT NULL,
    notes       VARCHAR(500),
    total       NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id           UUID PRIMARY KEY,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    version      BIGINT         NOT NULL DEFAULT 0,
    tenant_id    UUID           NOT NULL,
    order_id     UUID           NOT NULL REFERENCES orders (id),
    menu_item_id UUID REFERENCES menu_items (id),
    name         VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL DEFAULT 1,
    unit_price   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    notes        VARCHAR(300),
    status       VARCHAR(30)    NOT NULL
);

CREATE TABLE payments (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    version     BIGINT         NOT NULL DEFAULT 0,
    tenant_id   UUID           NOT NULL,
    order_id    UUID           NOT NULL REFERENCES orders (id),
    method      VARCHAR(30)    NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL DEFAULT 0
);

-- Tenant-scoped lookup indexes.
CREATE INDEX idx_staff_tenant ON staff_members (tenant_id);
CREATE INDEX idx_categories_tenant ON categories (tenant_id);
CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_menu_items_tenant ON menu_items (tenant_id);
CREATE INDEX idx_tables_tenant ON restaurant_tables (tenant_id);
CREATE INDEX idx_orders_tenant_status ON orders (tenant_id, status);
CREATE INDEX idx_order_items_tenant_status ON order_items (tenant_id, status);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_payments_order ON payments (order_id);
