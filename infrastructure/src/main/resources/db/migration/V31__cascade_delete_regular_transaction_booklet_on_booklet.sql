-- GDPR Art. 5(1)(e) and Art. 17 — erasing an account must not fail on its booklets.
--
-- BookletResource.regularTransactions is the inverse side of the @ManyToMany, so Hibernate never
-- deletes the regular_transaction_booklet rows of a booklet removed through the user cascade.
-- The id_booklet foreign key still dates from V1 (monthly_transaction_booklet.id_account, renamed
-- by V4 and V18) and had no ON DELETE CASCADE: deleting such a user failed, and with it the nightly
-- retention purge. V23 only cascaded the transaction_id side.
ALTER TABLE regular_transaction_booklet
    DROP CONSTRAINT IF EXISTS monthly_transaction_booklet_id_account_fkey;

ALTER TABLE regular_transaction_booklet
    DROP CONSTRAINT IF EXISTS regular_transaction_booklet_id_booklet_fkey;

ALTER TABLE regular_transaction_booklet
    ADD CONSTRAINT regular_transaction_booklet_id_booklet_fkey
        FOREIGN KEY (id_booklet) REFERENCES booklet (id_booklet) ON DELETE CASCADE;
