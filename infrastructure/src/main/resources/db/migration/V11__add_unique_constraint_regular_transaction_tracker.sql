-- Ensure one tracker row per (regular_transaction_id, booklet_id)
-- 1) Remove potential historical duplicates, keeping the oldest row.
-- 2) Add a unique constraint to make upsert idempotent under concurrency.

DELETE FROM regular_transaction_tracker t
WHERE t.id IN (
    SELECT t2.id
    FROM regular_transaction_tracker t2
    JOIN regular_transaction_tracker t3
      ON t2.regular_transaction_id = t3.regular_transaction_id
     AND t2.booklet_id = t3.booklet_id
     AND t2.id > t3.id
);

ALTER TABLE regular_transaction_tracker
    ADD CONSTRAINT uk_regular_transaction_tracker_regular_booklet
    UNIQUE (regular_transaction_id, booklet_id);

