-- Add excluded_months column to regular_transaction_tracker table
-- This column stores the list of months (format: YYYY-MM) where the user has explicitly
-- deleted a confirmed regular transaction and doesn't want it to regenerate

ALTER TABLE regular_transaction_tracker
ADD COLUMN excluded_months TEXT;

COMMENT ON COLUMN regular_transaction_tracker.excluded_months IS 'Comma-separated list of YearMonth (YYYY-MM) where the user deleted a confirmed transaction and does not want it to regenerate';
