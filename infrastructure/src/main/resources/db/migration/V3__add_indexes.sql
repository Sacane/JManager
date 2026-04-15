CREATE INDEX IF NOT EXISTS idx_account_owner_label ON account(owner_id_user, label);

CREATE INDEX IF NOT EXISTS idx_sheet_account ON sheet(account_id_account);

CREATE INDEX IF NOT EXISTS idx_tag_personal_owner ON tag_personal_resource(owner_id_user);
CREATE INDEX IF NOT EXISTS idx_tag_personal_name_owner ON tag_personal_resource(name, owner_id_user);

CREATE INDEX IF NOT EXISTS idx_tracker_transaction_booklet ON regular_transaction_tracker(regular_transaction_id, booklet_id);

CREATE INDEX IF NOT EXISTS idx_tracker_booklet ON regular_transaction_tracker(booklet_id);

CREATE INDEX IF NOT EXISTS idx_monthly_transaction_booklet_transaction ON monthly_transaction_booklet(transaction_id);
CREATE INDEX IF NOT EXISTS idx_monthly_transaction_booklet_account ON monthly_transaction_booklet(id_account);


