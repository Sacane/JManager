ALTER TABLE account RENAME TO booklet;
ALTER TABLE booklet RENAME COLUMN id_account TO id_booklet;
ALTER TABLE sheet RENAME COLUMN account_id_account TO account_id_booklet;
