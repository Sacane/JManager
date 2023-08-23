INSERT INTO Sheet (id_sheet, amount, date, is_entry, label_sheet)
VALUES
    (1, 100.50, '2023-08-01', true, 'Entry 1'),
    (2, -50.75, '2023-08-02', false, 'Expense 1'),
    (3, 200.00, '2023-08-03', true, 'Entry 2'),
    (4, -75.20, '2023-08-04', false, 'Expense 2'),
    (5, 300.25, '2023-08-05', true, 'Entry 3'),
    (6, -30.00, '2023-08-06', false, 'Expense 3');


INSERT INTO account_sheet VALUES (21,1);
INSERT INTO account_sheet VALUES (21,2);
INSERT INTO account_sheet VALUES (21,3);
INSERT INTO account_sheet VALUES (21,4);
INSERT INTO account_sheet VALUES (21,5);
INSERT INTO account_sheet VALUES (21,6);