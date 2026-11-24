-- ==============================================================================
-- JaldiShop Migration V39: Harden settings constraints & store settings audit index
-- Version: V39__harden_settings_and_store_audit.sql
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_platform_maintenance_notice_nonempty'
    ) THEN
        ALTER TABLE platform_settings
            ADD CONSTRAINT chk_platform_maintenance_notice_nonempty
            CHECK (NOT maintenance_mode OR length(btrim(maintenance_notice)) > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_store_settings_policies_nonempty'
    ) THEN
        ALTER TABLE store_settings
            ADD CONSTRAINT chk_store_settings_policies_nonempty
            CHECK (length(btrim(cancellation_policy)) > 0 AND length(btrim(refund_policy)) > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_target_created
    ON admin_audit_logs (target_type, target_id, created_at DESC);
