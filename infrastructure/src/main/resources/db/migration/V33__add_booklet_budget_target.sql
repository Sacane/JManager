-- The monthly budget the user aims at for a booklet (UX-23), until now kept in the browser only.
-- Null when none is set. On the booklet row, so it goes with the booklet when it is deleted.
ALTER TABLE booklet
    ADD COLUMN IF NOT EXISTS budget_target NUMERIC(12, 2) NULL;
