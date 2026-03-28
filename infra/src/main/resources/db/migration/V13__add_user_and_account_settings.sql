ALTER TABLE account
    ADD COLUMN monthly_period_start_day INTEGER NOT NULL DEFAULT 1;

ALTER TABLE account
    ADD CONSTRAINT chk_account_monthly_period_start_day
        CHECK (monthly_period_start_day BETWEEN 1 AND 31);

ALTER TABLE user_resource
    ADD COLUMN projection_window_days INTEGER NOT NULL DEFAULT 15;

ALTER TABLE user_resource
    ADD CONSTRAINT chk_user_projection_window_days
        CHECK (projection_window_days BETWEEN 7 AND 60);
