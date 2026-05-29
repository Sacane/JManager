-- Feature flags — runtime-controlled ON/OFF switches persisted in the database.
-- The primary key is the flag's canonical name (FeatureKey enum name stored as VARCHAR).
-- No UUID surrogate key is needed: the flag name is globally unique and immutable.
-- Default enabled = false (safe-by-default); the startup seeder inserts missing rows on each deploy.

CREATE TABLE IF NOT EXISTS feature_flag (
    key     VARCHAR(100) NOT NULL,
    enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_feature_flag PRIMARY KEY (key)
);
