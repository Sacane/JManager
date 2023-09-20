What is not **OPTIONAL** has **NOT NULL** constraint

## User

- ID: ``Long`` **UNIQUE**
- username: `string`
- password: ``byteArray``
- email: ``string`` **UNIQUE | OPTIONAL**
- accounts: ``set<Account>``
- categories: ``set<Category>``

## Account

- ID: ``Long`` **UNIQUE**
- amount: ``Long``
- labelAccount: ``string`` **UNIQUE**
- sheets: ``set<Sheet>``

## Sheet 

- ID: ``Long`` UNIQUE
- label ``String`` 
- date ``LocalDate``
- expenses: ``Double``
- income: ``Double``
- accountSold: ``Double``
- category: ``Category``
- position: ``Int`` **UNIQUE**


## TODO 

- Check all uniques conditions
- implements the token lifecycle process