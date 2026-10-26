-- GDPR Art. 17 — Right to erasure.
-- Deleting an account must leave nothing behind. Two gaps remained after V23:

-- 1. Deleting a booklet (and therefore an account) failed while a regular transaction was still
--    attached to it: the join-table FK to booklet had no ON DELETE action.
ALTER TABLE regular_transaction_booklet
    DROP CONSTRAINT IF EXISTS monthly_transaction_booklet_id_account_fkey;

ALTER TABLE regular_transaction_booklet
    ADD CONSTRAINT regular_transaction_booklet_id_booklet_fkey
        FOREIGN KEY (id_booklet) REFERENCES booklet (id_booklet) ON DELETE CASCADE;

-- 2. regular_transaction_tracker.booklet_id had no FK at all, so trackers outlived their booklet.
--    Remove the trackers already orphaned by past deletions, then let the database cascade.
DELETE FROM regular_transaction_tracker t
WHERE NOT EXISTS (SELECT 1 FROM booklet b WHERE b.id_booklet = t.booklet_id);

ALTER TABLE regular_transaction_tracker
    ADD CONSTRAINT regular_transaction_tracker_booklet_id_fkey
        FOREIGN KEY (booklet_id) REFERENCES booklet (id_booklet) ON DELETE CASCADE;

-- 3. Recurrence rules and frequency properties are owned one-to-one by a regular transaction, but the
--    FK points from the transaction to them, so the V23 cascade left them behind. Account deletion now
--    removes regular transactions through JPA (which cascades to both); this cleans what past
--    deletions orphaned. recurrence_days_of_week follows its rule by cascade.
DELETE FROM recurrence_rule r
WHERE NOT EXISTS (SELECT 1 FROM regular_transaction t WHERE t.recurrence_rule_id = r.id);

DELETE FROM frequency_property_entity f
WHERE NOT EXISTS (SELECT 1 FROM regular_transaction t WHERE t.frequency_property_id = f.id);
