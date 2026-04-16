ALTER TABLE user_resource
    ADD COLUMN creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
ADD COLUMN is_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE user_resource
SET creation_date = NOW()
WHERE creation_date IS NULL;

UPDATE user_resource
SET is_enabled = TRUE
WHERE is_enabled IS NULL;