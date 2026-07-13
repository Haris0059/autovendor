-- OLX has no endpoint listing active sponsorships/discounts (probed July 2026:
-- /sponsored, /discounts etc. all 404), so rows created through AutoVendor are
-- tracked here. ended_at NULL + ends_at > now() = active; ended/superseded rows
-- are kept for history/analytics. No unique active-per-listing constraint --
-- natural expiry (ends_at passing) would block re-sponsoring; the service
-- supersedes active rows instead.

CREATE TABLE sponsorships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    olx_account_id BIGINT NOT NULL REFERENCES olx_accounts(id) ON DELETE CASCADE,
    olx_listing_id BIGINT NOT NULL,
    type INT NOT NULL,
    days INT NOT NULL,
    refresh_every INT NOT NULL,
    locations VARCHAR(255) NOT NULL DEFAULT '',
    price_total NUMERIC(12,2),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ends_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sponsorships_user_active ON sponsorships(user_id, ends_at) WHERE ended_at IS NULL;
CREATE INDEX idx_sponsorships_listing_active ON sponsorships(olx_listing_id) WHERE ended_at IS NULL;

CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    olx_account_id BIGINT NOT NULL REFERENCES olx_accounts(id) ON DELETE CASCADE,
    olx_listing_id BIGINT NOT NULL,
    original_price NUMERIC(12,2) NOT NULL,
    discount_price NUMERIC(12,2) NOT NULL,
    days INT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ends_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discounts_user_active ON discounts(user_id, ends_at) WHERE ended_at IS NULL;
CREATE INDEX idx_discounts_listing_active ON discounts(olx_listing_id) WHERE ended_at IS NULL;
