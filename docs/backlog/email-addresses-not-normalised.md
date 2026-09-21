# Email addresses are stored and matched with their case

## Observation

No code normalises an email address. Registration stores it as typed, `user_resource.email` is
`VARCHAR(255) UNIQUE` (case-sensitive), and sign-in looks it up with an exact
`UserPostgresRepository.findByEmail`. Consequences:

- a user registered as `Johan@Example.com` cannot sign in by typing `johan@example.com`;
- two accounts can exist for `Johan@Example.com` and `johan@example.com`, which are one mailbox.

UX-21 works around it for the reset only (case-insensitive lookup, and no email at all when two
accounts match). Sign-in and registration keep the problem.

## Location

- `domain/.../port/input/user/RegisterUserUseCase.kt`, `LoginUseCase.kt`
- `infrastructure/.../spi/repositories/UserPostgresRepository.kt` (`findByEmail`)
- `V1__init_schema.sql` (`email VARCHAR(255) UNIQUE`)

## Expected behaviour

Addresses are normalised (trimmed, lower-cased) when stored and when looked up, and uniqueness holds
regardless of case — for example with a unique index on `lower(email)`.

## Impact

Medium: failed sign-ins that look like a wrong password, and possible duplicate accounts for one
mailbox. **Before migrating**, count the addresses that collide once lower-cased in production — a
unique index on `lower(email)` cannot be created while duplicates exist, and merging accounts is a
product decision, not a script.
