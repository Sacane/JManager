
CREATE TABLE IF NOT EXISTS recurrence_rule (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    day_of_month INTEGER,
    month INTEGER,
    CONSTRAINT check_recurrence_type CHECK (type IN ('MONTHLY', 'YEARLY', 'WEEKLY', 'DAILY'))
);

CREATE TABLE IF NOT EXISTS recurrence_days_of_week (
    recurrence_rule_id VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    FOREIGN KEY (recurrence_rule_id) REFERENCES recurrence_rule(id) ON DELETE CASCADE,
    CONSTRAINT check_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

ALTER TABLE monthly_regular_transaction RENAME TO regular_transaction;

ALTER TABLE regular_transaction ADD COLUMN recurrence_rule_id VARCHAR(255);

INSERT INTO recurrence_rule (id, type, day_of_month)
SELECT
    CONCAT('recurrence-', transaction_id),
    'MONTHLY',
    COALESCE(repeat_day, 1)
FROM regular_transaction;

UPDATE regular_transaction
SET recurrence_rule_id = CONCAT('recurrence-', transaction_id);

ALTER TABLE regular_transaction ALTER COLUMN recurrence_rule_id SET NOT NULL;

ALTER TABLE regular_transaction
ADD CONSTRAINT fk_regular_transaction_recurrence_rule
FOREIGN KEY (recurrence_rule_id) REFERENCES recurrence_rule(id);

ALTER TABLE regular_transaction DROP COLUMN IF EXISTS repeat_day;

ALTER TABLE monthly_transaction_booklet RENAME TO regular_transaction_booklet;

CREATE INDEX IF NOT EXISTS idx_regular_transaction_recurrence_rule
ON regular_transaction(recurrence_rule_id);

CREATE INDEX IF NOT EXISTS idx_recurrence_rule_type
ON recurrence_rule(type);

CREATE INDEX IF NOT EXISTS idx_recurrence_days_of_week_rule
ON recurrence_days_of_week(recurrence_rule_id);

