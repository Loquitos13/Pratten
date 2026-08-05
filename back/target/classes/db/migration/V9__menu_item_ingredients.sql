-- Receitas: ingredientes de stock consumidos por prato do menu.

CREATE TABLE menu_item_ingredients (
    id                   UUID PRIMARY KEY,
    created_at           TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ,
    version              BIGINT          NOT NULL DEFAULT 0,
    tenant_id            UUID            NOT NULL,
    menu_item_id         UUID            NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    product_id           UUID            NOT NULL REFERENCES products (id),
    quantity_per_serving NUMERIC(12, 3)  NOT NULL CHECK (quantity_per_serving > 0),
    CONSTRAINT uq_menu_item_ingredient UNIQUE (tenant_id, menu_item_id, product_id)
);

CREATE INDEX idx_menu_item_ingredients_tenant ON menu_item_ingredients (tenant_id);
CREATE INDEX idx_menu_item_ingredients_menu_item ON menu_item_ingredients (menu_item_id);
