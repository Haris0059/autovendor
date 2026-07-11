CREATE TABLE woo_stores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    store_url VARCHAR(2048) NOT NULL,
    encrypted_api_key BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, store_url)
);

CREATE INDEX idx_woo_stores_user_id ON woo_stores(user_id);
