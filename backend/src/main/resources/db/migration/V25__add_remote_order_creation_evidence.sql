-- Expand only: historical financial states and V24 snapshots remain unchanged.
ALTER TABLE payment_attempts ADD COLUMN remote_order_creation_state VARCHAR(30);
UPDATE payment_attempts SET remote_order_creation_state = 'UNKNOWN' WHERE checkout_mode = 'ORDERS';
ALTER TABLE payment_attempts ADD CONSTRAINT ck_remote_order_creation_state CHECK (
    (checkout_mode = 'ORDERS' AND remote_order_creation_state IS NOT NULL
        AND remote_order_creation_state IN ('PREPARED', 'UNKNOWN', 'CREATED', 'REJECTED_CONFIRMED'))
    OR (checkout_mode <> 'ORDERS' AND remote_order_creation_state IS NULL)
);
ALTER TABLE payment_attempts ADD CONSTRAINT ck_created_order_evidence CHECK (
    remote_order_creation_state IS DISTINCT FROM 'CREATED'
    OR (NULLIF(BTRIM(provider_order_id), '') IS NOT NULL AND NULLIF(BTRIM(init_point), '') IS NOT NULL)
);

-- No bodies, URLs, headers, tokens or customer fields.
CREATE TABLE remote_order_creation_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_attempt_id UUID NOT NULL REFERENCES payment_attempts(id) ON DELETE CASCADE,
    previous_state VARCHAR(30),
    new_state VARCHAR(30) NOT NULL,
    evidence_code VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_order_creation_events_attempt ON remote_order_creation_events(payment_attempt_id, id);
INSERT INTO remote_order_creation_events(payment_attempt_id, new_state, evidence_code)
    SELECT id, 'UNKNOWN', 'HISTORICAL_UNVERIFIED' FROM payment_attempts WHERE checkout_mode = 'ORDERS';

CREATE FUNCTION guard_remote_order_creation_state() RETURNS trigger AS $$
BEGIN
    -- Older writers omit the column; absence is never proof of non-dispatch.
    IF NEW.checkout_mode = 'ORDERS' AND NEW.remote_order_creation_state IS NULL THEN
        NEW.remote_order_creation_state := 'UNKNOWN';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF OLD.remote_order_creation_state IN ('CREATED', 'REJECTED_CONFIRMED')
            AND NEW.remote_order_creation_state IS DISTINCT FROM OLD.remote_order_creation_state THEN
            RAISE EXCEPTION 'confirmed order creation evidence cannot be downgraded';
        END IF;
        IF OLD.remote_order_creation_state = 'UNKNOWN' AND NEW.remote_order_creation_state = 'PREPARED' THEN
            RAISE EXCEPTION 'unknown order creation cannot return to prepared';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER guard_remote_order_creation_state BEFORE INSERT OR UPDATE ON payment_attempts
    FOR EACH ROW EXECUTE FUNCTION guard_remote_order_creation_state();

CREATE FUNCTION audit_remote_order_creation_state() RETURNS trigger AS $$
DECLARE previous VARCHAR(30);
BEGIN
    IF TG_OP = 'UPDATE' THEN previous := OLD.remote_order_creation_state; END IF;
    IF NEW.remote_order_creation_state IS NOT NULL
        AND NEW.remote_order_creation_state IS DISTINCT FROM previous THEN
        INSERT INTO remote_order_creation_events(payment_attempt_id, previous_state, new_state, evidence_code)
        VALUES (NEW.id, previous, NEW.remote_order_creation_state,
            CASE NEW.remote_order_creation_state
                WHEN 'PREPARED' THEN 'LOCAL_PREPARATION'
                WHEN 'UNKNOWN' THEN 'NO_CONFIRMED_RESULT'
                WHEN 'CREATED' THEN 'VALIDATED_CREATION_RESPONSE'
                WHEN 'REJECTED_CONFIRMED' THEN 'INVALID_TOTAL_AMOUNT'
            END);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER audit_remote_order_creation_state AFTER INSERT OR UPDATE ON payment_attempts
    FOR EACH ROW EXECUTE FUNCTION audit_remote_order_creation_state();

CREATE FUNCTION prevent_order_creation_event_update() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'order creation events are immutable';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER remote_order_creation_events_immutable BEFORE UPDATE ON remote_order_creation_events
    FOR EACH ROW EXECUTE FUNCTION prevent_order_creation_event_update();
