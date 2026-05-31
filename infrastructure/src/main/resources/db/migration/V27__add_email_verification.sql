-- Email verification state on the user aggregate.
-- All existing rows are backfilled to true: they pre-date the verification feature
-- and were created by an admin, so they are implicitly trusted.
ALTER TABLE user_resource
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE user_resource SET email_verified = true;

-- Verification tokens: one active token per user, valid until expiresAt.
-- ON DELETE CASCADE ensures tokens are cleaned up automatically when a user is deleted.
CREATE TABLE IF NOT EXISTS email_verification_token (
    token      VARCHAR(64)  NOT NULL,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_email_verification_token PRIMARY KEY (token),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id)
        REFERENCES user_resource (id_user)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_evt_user_id ON email_verification_token (user_id);
