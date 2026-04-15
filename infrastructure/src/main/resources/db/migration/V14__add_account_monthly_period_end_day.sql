ALTER TABLE account
    ADD COLUMN monthly_period_end_day INTEGER;

ALTER TABLE account
    ADD CONSTRAINT chk_account_monthly_period_end_day
        CHECK (monthly_period_end_day IS NULL OR monthly_period_end_day BETWEEN 1 AND 31);
