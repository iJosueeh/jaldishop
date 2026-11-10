-- Additive migration: existing accounts and JWTs remain version zero until a reset.
ALTER TABLE users ADD COLUMN credentials_version BIGINT NOT NULL DEFAULT 0 CHECK (credentials_version >= 0);

-- One outstanding token per user; tokens are random 256-bit values, persisted only as SHA-256.
CREATE TABLE password_reset_tokens (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    expires_at TIMESTAMPTZ NOT NULL
);
