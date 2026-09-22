-- When the user's password last changed, in UTC. Access tokens issued before it are refused.
-- Existing rows stay null, meaning no change has revoked anything yet.
ALTER TABLE user_resource
    ADD COLUMN IF NOT EXISTS credentials_changed_at TIMESTAMP NULL;
