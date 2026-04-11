-- Speeds up monthly listing and range queries per booklet/account
-- Used by: filtering sheets by account_id_account + date range

CREATE INDEX IF NOT EXISTS idx_sheet_account_date ON sheet(account_id_account, date);

