-- ==============================================================================
-- JaldiShop Migration V42: Customer Email Verification
-- Version: V42__add_email_verification.sql
-- Description:
--   1. Add email_verified and email_verified_at to users.
--      Existing/seed accounts are marked verified for backward compatibility;
--      column default is then set to FALSE for new accounts.
--   2. Create email_verification_tokens table (one active token per user,
--      stored only as SHA-256 hex hash).
-- ==============================================================================

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_verified_at TIMESTAMPTZ;

UPDATE users
SET email_verified_at = created_at
WHERE email_verified = TRUE AND email_verified_at IS NULL;

ALTER TABLE users
    ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE TABLE email_verification_tokens (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens (expires_at);
