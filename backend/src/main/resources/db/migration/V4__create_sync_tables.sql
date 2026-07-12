CREATE TABLE product_links (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    olx_account_id BIGINT NOT NULL REFERENCES olx_accounts(id) ON DELETE CASCADE,
    woo_store_id BIGINT NOT NULL REFERENCES woo_stores(id) ON DELETE CASCADE,
    olx_listing_id BIGINT,
    woo_product_id BIGINT NOT NULL,
    sync_direction VARCHAR(20) NOT NULL,
    woo_hash VARCHAR(64),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (woo_store_id, woo_product_id)
);

CREATE INDEX idx_product_links_user_id ON product_links(user_id);

CREATE TABLE category_mappings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    woo_category_id BIGINT NOT NULL,
    woo_category_name VARCHAR(255) NOT NULL,
    olx_category_id BIGINT NOT NULL,
    olx_category_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, woo_category_id)
);

CREATE INDEX idx_category_mappings_user_id ON category_mappings(user_id);

-- user_id, olx_account_id and woo_store_id are denormalized: product_link_id is
-- nullable (ON DELETE SET NULL keeps history after a link is deleted), so logs
-- must be scannable and filterable without a join.
CREATE TABLE sync_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_link_id BIGINT REFERENCES product_links(id) ON DELETE SET NULL,
    olx_account_id BIGINT,
    woo_store_id BIGINT,
    action VARCHAR(30) NOT NULL,
    status VARCHAR(10) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_logs_link_created ON sync_logs(product_link_id, created_at DESC);
CREATE INDEX idx_sync_logs_user_created ON sync_logs(user_id, created_at DESC);
