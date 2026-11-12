-- ==============================================================================
-- JaldiShop Migration V28: Primary Admin Flag, Admin Invitations and Audit Logs
-- Version: V28__add_primary_admin_invitations_and_audit.sql
-- Description:
--   1. Add is_primary_admin column with partial unique index to users.
--   2. Create admin_invitations table for secure single-use invitation lifecycle.
--   3. Create admin_audit_logs table for administrative operation audit trail.
--   Strictly structural and additive. No DML operations.
-- ==============================================================================

-- 1. PRIMARY ADMIN FLAG ON USERS
ALTER TABLE users ADD COLUMN is_primary_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Enforce exactly zero or one primary admin across the entire database
CREATE UNIQUE INDEX uq_users_primary_admin ON users (is_primary_admin) WHERE is_primary_admin = TRUE;

-- 2. ADMIN INVITATIONS LIFECYCLE
CREATE TABLE admin_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT ck_admin_invitations_accepted CHECK (status != 'ACCEPTED' OR accepted_at IS NOT NULL),
    CONSTRAINT ck_admin_invitations_revoked CHECK (status != 'REVOKED' OR revoked_at IS NOT NULL)
);

-- At most one active PENDING invitation per target email address
CREATE UNIQUE INDEX uq_admin_invitations_pending_email ON admin_invitations (email) WHERE status = 'PENDING';
CREATE INDEX idx_admin_invitations_token_hash ON admin_invitations (token_hash);
CREATE INDEX idx_admin_invitations_expires_at ON admin_invitations (expires_at);
CREATE INDEX idx_admin_invitations_invited_by ON admin_invitations (invited_by_user_id);

-- 3. ADMINISTRATIVE AUDIT LOGS
CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(254),
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILURE', 'DENIED')),
    ip_address VARCHAR(45),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_logs_actor ON admin_audit_logs(actor_id);
CREATE INDEX idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);
CREATE INDEX idx_admin_audit_logs_target ON admin_audit_logs(target_type, target_id);
