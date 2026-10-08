-- Password reset tokens, stored as the SHA-256 digest of the raw token only.
-- The unique user_id makes "one active token per user" a schema rule, not only a habit of the code.
-- ON DELETE CASCADE removes the token with its user.
CREATE TABLE IF NOT EXISTS password_reset_token (
    token_hash CHAR(64)  NOT NULL,
    user_id    UUID      NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_password_reset_token PRIMARY KEY (token_hash),
    CONSTRAINT uq_password_reset_token_user UNIQUE (user_id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
        REFERENCES user_resource (id_user)
        ON DELETE CASCADE
);
