ALTER TABLE sheet RENAME TO transactions;
ALTER TABLE transactions RENAME COLUMN id_sheet TO id_transaction;
ALTER TABLE transactions RENAME COLUMN label_sheet TO label_transaction;

ALTER TABLE default_tag_resource_linked_transaction
    RENAME COLUMN linked_transaction_id_sheet TO linked_transaction_id_transaction;

ALTER TABLE tag_personal_resource_linked_transaction
    RENAME COLUMN linked_transaction_id_sheet TO linked_transaction_id_transaction;

ALTER INDEX IF EXISTS idx_sheet_account RENAME TO idx_transaction_booklet;
ALTER INDEX IF EXISTS idx_sheet_account_date RENAME TO idx_transaction_booklet_date;
ALTER INDEX IF EXISTS idx_sheet_regular_transaction_id RENAME TO idx_transaction_regular_transaction_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sheet_pkey') THEN
        ALTER TABLE transactions RENAME CONSTRAINT sheet_pkey TO transactions_pkey;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sheet_regular_transaction') THEN
        ALTER TABLE transactions RENAME CONSTRAINT fk_sheet_regular_transaction TO fk_transaction_regular_transaction;
    END IF;
END $$;
