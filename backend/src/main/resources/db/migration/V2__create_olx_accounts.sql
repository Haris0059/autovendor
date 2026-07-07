CREATE TABLE olx_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(255) NOT NULL,
    encrypted_password BYTEA NOT NULL,
    olx_user_id BIGINT,
    default_city_id BIGINT,
    token_ciphertext BYTEA,
    token_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, username)
);

CREATE INDEX idx_olx_accounts_user_id ON olx_accounts(user_id);
