-- Migration V8: Convert regular_transaction_id from VARCHAR(255) to UUID
-- This migration aligns the column type with regular_transaction.transaction_id
-- Historical context: this migration targets table sheet (renamed to transactions in V20)

-- Step 1: Clean invalid data (IDs that are not valid UUIDs)
-- Remove previsional transactions with invalid or non-existing IDs
DELETE FROM sheet
WHERE regular_transaction_id IS NOT NULL
AND (
    -- ID is not a valid UUID
    regular_transaction_id !~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    OR
    -- ID does not match any regular transaction
    NOT EXISTS (
        SELECT 1 FROM regular_transaction rt
        WHERE rt.transaction_id::text = regular_transaction_id
    )
);

-- Step 2: Create a temporary UUID column
ALTER TABLE sheet ADD COLUMN regular_transaction_id_uuid UUID;

-- Step 3: Migrate valid data to the new column
UPDATE sheet
SET regular_transaction_id_uuid = regular_transaction_id::uuid
WHERE regular_transaction_id IS NOT NULL
AND regular_transaction_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

-- Step 4: Drop the old column and index
DROP INDEX IF EXISTS idx_sheet_regular_transaction_id;
ALTER TABLE sheet DROP COLUMN regular_transaction_id;

-- Step 5: Rename the new column
ALTER TABLE sheet RENAME COLUMN regular_transaction_id_uuid TO regular_transaction_id;

-- Step 6: Recreate the index on the UUID column
CREATE INDEX idx_sheet_regular_transaction_id ON sheet(regular_transaction_id);

-- Step 7: Add a foreign key constraint to enforce referential integrity
-- This prevents creating previsional transactions with a non-existing ID
ALTER TABLE sheet
ADD CONSTRAINT fk_sheet_regular_transaction
FOREIGN KEY (regular_transaction_id)
REFERENCES regular_transaction(transaction_id)
ON DELETE SET NULL;  -- If the regular transaction is deleted, set NULL

-- Step 8: Add an explanatory column comment
COMMENT ON COLUMN sheet.regular_transaction_id IS
'Source regular transaction UUID (when this transaction is generated from a regular transaction). UUID type aligns with regular_transaction.transaction_id';

-- Post-migration verification
DO $$
DECLARE
    invalid_count INTEGER;
    orphan_count INTEGER;
BEGIN
    -- Count transactions with orphan regular_transaction_id values (should be zero)
    SELECT COUNT(*) INTO invalid_count
    FROM sheet
    WHERE regular_transaction_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM regular_transaction rt
        WHERE rt.transaction_id = regular_transaction_id
    );

    IF invalid_count > 0 THEN
        RAISE WARNING 'Warning: % previsional transaction(s) with orphan regular_transaction_id detected', invalid_count;
    ELSE
        RAISE NOTICE 'Migration successful: all regular_transaction_id values are valid';
    END IF;

    -- Display summary
    SELECT COUNT(*) INTO orphan_count
    FROM sheet
    WHERE regular_transaction_id IS NOT NULL;

    RAISE NOTICE 'Total previsional transactions with regular_transaction_id: %', orphan_count;
END $$;

