-- Speeds up monthly listing and range queries per booklet/account
-- Legacy schema step: targets table sheet (renamed to transactions in V20)
-- Used by: filtering transactions by account_id_account + date range

CREATE INDEX IF NOT EXISTS idx_sheet_account_date ON sheet(account_id_account, date);

