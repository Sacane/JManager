-- Add regular_transaction_id column to sheet table to track which regular transaction generated a previsional transaction
ALTER TABLE sheet ADD COLUMN regular_transaction_id VARCHAR(255);

-- Add index for better query performance when checking for duplicates
CREATE INDEX idx_sheet_regular_transaction_id ON sheet(regular_transaction_id);

