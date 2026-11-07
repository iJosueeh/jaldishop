ALTER TABLE payment_attempts ADD CONSTRAINT uq_attempt_creation_context
    UNIQUE (id, checkout_mode, idempotency_key);

CREATE TABLE checkout_creation_requests (
    payment_attempt_id UUID PRIMARY KEY,
    checkout_mode VARCHAR(20) NOT NULL CHECK (checkout_mode IN ('PREFERENCES', 'ORDERS')),
    idempotency_key VARCHAR(64) NOT NULL,
    format_version SMALLINT NOT NULL CHECK (format_version = 1),
    request_body TEXT NOT NULL CHECK (jsonb_typeof(request_body::jsonb) = 'object'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_checkout_creation_context FOREIGN KEY (payment_attempt_id, checkout_mode, idempotency_key)
        REFERENCES payment_attempts(id, checkout_mode, idempotency_key) ON DELETE CASCADE
);

CREATE FUNCTION prevent_checkout_creation_request_update() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'checkout creation requests are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER checkout_creation_requests_immutable
    BEFORE UPDATE ON checkout_creation_requests
    FOR EACH ROW EXECUTE FUNCTION prevent_checkout_creation_request_update();
