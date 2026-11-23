-- ==============================================================================
-- JaldiShop Migration V34: Encrypt Sensitive Payout Destinations and Blind Index
-- Version: V34__encrypt_sensitive_payout_destinations.sql
-- Description:
--   1. Expands column sizes in merchant_payout_destinations to hold AES-256-GCM
--      Base64 ciphertexts (IV 12B + Ciphertext + Tag 16B).
--   2. Adds blind index column for secure duplicate checks without decrypting.
-- ==============================================================================

ALTER TABLE merchant_payout_destinations
    ALTER COLUMN account_cci TYPE VARCHAR(255),
    ALTER COLUMN account_number TYPE VARCHAR(255),
    ALTER COLUMN phone_number TYPE VARCHAR(255),
    ALTER COLUMN account_holder_doc_number TYPE VARCHAR(255);

ALTER TABLE merchant_payout_destinations
    ADD COLUMN IF NOT EXISTS account_cci_blind_index VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_payout_dest_cci_blind
    ON merchant_payout_destinations (account_cci_blind_index);
