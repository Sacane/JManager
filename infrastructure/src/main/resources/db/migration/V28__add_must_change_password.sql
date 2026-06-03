-- Mandatory-password-change flag, set to true for admin-created accounts.
-- Existing rows default to false: they have already chosen their own password.
ALTER TABLE user_resource
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
