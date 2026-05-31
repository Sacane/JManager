# JManager — Features

> Features are derived from the domain application ports (`domain/port/api/*Feature.kt`).
> Each feature is expressed in Gherkin format (Given/When/Then).

---

## Feature: User Authentication

As a user, I want to authenticate so that I can securely access my personal finance data.

### Scenario: Successful login
```gherkin
Given a registered user with pseudonym "alice" and a valid password
When the user logs in with pseudonym "alice" and the correct password
Then the system returns a UserToken containing an access token and a refresh token
```

### Scenario: Login with wrong password
```gherkin
Given a registered user with pseudonym "alice"
When the user logs in with pseudonym "alice" and an incorrect password
Then the system returns an unauthorized failure
```

### Scenario: Login with unknown pseudonym
```gherkin
Given no user exists with pseudonym "unknown"
When the user logs in with pseudonym "unknown"
Then the system returns a not-found failure
```

### Scenario: Logout
```gherkin
Given an authenticated user with a valid session token
When the user logs out
Then the session is invalidated
And the token can no longer be used for authenticated requests
```

### Scenario: Refresh session
```gherkin
Given an authenticated user with a valid refresh token
When the user requests a session refresh
Then the system returns a new UserToken with a fresh access token
```

---

## Feature: User Registration

As a visitor, I want to register an account so that I can start managing my finances.

### Scenario: Successful registration
```gherkin
Given no user exists with username "bob"
When a visitor registers with username "bob", password "secret123", confirmPassword "secret123", and email "bob@example.com"
Then a new User is created and returned
And the password is stored in hashed form
And the user's subscriptionPlan is BETA_TESTER
And a welcome email is sent asynchronously to "bob@example.com"
```

### Scenario: Registration with mismatched passwords
```gherkin
Given a visitor provides password "secret123" and confirmPassword "other456"
When the visitor attempts to register
Then the system returns a validation failure
And no welcome email is sent
```

### Scenario: Registration without email is rejected
```gherkin
Given a visitor submits a registration request with no email field
When the visitor attempts to register
Then the system returns a validation failure
```

> **Feature flag gate**: the entire registration flow is gated behind the `USER_REGISTRATION` flag.
> When the flag is disabled, `POST /api/user/create` returns HTTP 404 and the registration form is
> hidden in the UI. When enabled, all scenarios above apply.
>
> **Note**: self-registered users always go through the email verification flow (see *Email Verification*).
> The welcome email scenario above still applies — the verification email is sent in addition to it.

---

## Feature: Subscription Plan

As the system, I want every user to carry an explicit subscription plan so that features can be
gated per tier.

### Scenario: New user defaults to BETA_TESTER plan
```gherkin
Given a new user registers successfully
Then their subscriptionPlan is BETA_TESTER
```

### Scenario: Subscription plan is exposed in the registration response
```gherkin
Given a successful registration
When the API returns the created UserDTO
Then the response includes subscriptionPlan equal to "BETA_TESTER"
```

---

## Feature: Admin Bootstrap

As the system, I want to ensure an administrator user exists at startup.

### Scenario: Create admin when none exists
```gherkin
Given no admin user exists in the system
When createAdminIfNotExists is called with username "admin" and password "adminPass"
Then an admin User is created with the ADMIN role
```

### Scenario: Admin already exists
```gherkin
Given an admin user already exists
When createAdminIfNotExists is called
Then the system returns a failure indicating the admin already exists
```

---

## Feature: User Settings

As an authenticated user, I want to manage my dashboard settings so that projections and monthly cycles match my needs.

### Scenario: Retrieve current settings
```gherkin
Given an authenticated user with configured settings
When the user requests their settings
Then the system returns the current UserSettings including projection window and booklet cycles
```

### Scenario: Update settings
```gherkin
Given an authenticated user
When the user updates settings with projectionWindowDays 30 and booklet monthly cycles
Then the system persists the new settings
And returns the updated UserSettings
```

### Scenario: Update settings with invalid projection window
```gherkin
Given an authenticated user
When the user updates settings with projectionWindowDays outside the 7..60 range
Then the system returns a validation failure
```

---

## Feature: Booklet Management

As an authenticated user, I want to manage my booklets (accounts/registers) so that I can organize my finances.

### Scenario: Create a new booklet
```gherkin
Given an authenticated user with no booklet labelled "Savings"
When the user saves a booklet with label "Savings"
Then the booklet is persisted and returned with its generated ID
```

### Scenario: Create a booklet with duplicate label
```gherkin
Given an authenticated user who already has a booklet labelled "Savings"
When the user saves another booklet with label "Savings"
Then the system returns a failure indicating the label already exists
```

### Scenario: Find booklet by ID
```gherkin
Given an authenticated user who owns a booklet with a known ID
When the user requests the booklet by its ID
Then the system returns the corresponding Booklet
```

### Scenario: Find booklet by ID owned by another user
```gherkin
Given an authenticated user who does not own the booklet with the given ID
When the user requests that booklet
Then the system returns a failure
```

### Scenario: Find booklet by label
```gherkin
Given an authenticated user who owns a booklet labelled "Current"
When the user searches for a booklet by label "Current"
Then the system returns the matching Booklet
```

### Scenario: List all booklets
```gherkin
Given an authenticated user with multiple booklets
When the user requests all registered booklets
Then the system returns the complete list of the user's booklets
```

### Scenario: Edit a booklet
```gherkin
Given an authenticated user who owns a booklet
When the user edits the booklet with a new label
Then the updated booklet is persisted and returned
```

### Scenario: Delete a booklet
```gherkin
Given an authenticated user who owns a booklet
When the user deletes the booklet by its ID
Then the booklet and its associated data are removed
```

---

## Feature: Booklet Transaction Loading

As an authenticated user, I want to load transactions and balances for a specific month so that I can review my financial activity.

### Scenario: Load transactions for a given month
```gherkin
Given an authenticated user who owns a booklet with transactions in January 2025
When the user loads transactions for booklet ID, month January, year 2025
Then the system returns a BookletLoadingResult containing the transactions and computed balances
```

### Scenario: Load transactions generates provisional transactions from regular transactions
```gherkin
Given an authenticated user with a booklet linked to recurring regular transactions
And some regular transactions have not yet generated preview entries for the target month
When the user loads transactions for that month
Then the system generates provisional (preview) transactions for the missing periods
And includes them in the BookletLoadingResult
```

### Scenario: Load balances for a given month
```gherkin
Given an authenticated user who owns a booklet
When the user loads balances for booklet ID, month March, year 2025
Then the system returns BookletBalances with current and forecasted balance information
```

### Scenario: Regenerate deleted provisional transactions for the current month
```gherkin
Given an authenticated user who previously deleted provisional transactions for the current month
When the user requests regeneration of deleted provisional transactions for the current month and year
Then the system un-marks the month as excluded in the tracker
And recreates the previsional transactions (isPreview = true, persisted) without duplicating existing ones
```

### Scenario: Regenerate deleted virtual transactions for a future month
```gherkin
Given an authenticated user who previously deleted virtual transactions for a future month
When the user requests regeneration of deleted transactions for that future month and year
Then the system un-marks the month as excluded in the tracker
And returns the corresponding virtual transactions (computed on-the-fly, not persisted)
And no previsional transaction is created in the database
```

### Scenario: Regeneration for a past month has no effect
```gherkin
Given an authenticated user who requests regeneration for a past month
When the user requests regeneration of deleted transactions for that past month and year
Then the system returns an empty list
And the tracker is not modified
```

> **Golden rule**: a regular transaction can only generate **previsional transactions** (persisted) when the targeted month is the **current month**. For future months, only **virtual transactions** (computed, not persisted) are produced. Regeneration for past months has no effect.

---

## Feature: Transaction Management

As an authenticated user, I want to create, view, edit and delete transactions so that I can track my income and expenses.

### Scenario: Book a new transaction
```gherkin
Given an authenticated user who owns a booklet labelled "Current"
When the user books a transaction with a date, label, and amount on booklet "Current"
Then the transaction is persisted
And the system returns a TransactionResumeResult with the updated booklet balance
```

### Scenario: Retrieve transactions by month and year (paginated)
```gherkin
Given an authenticated user with transactions in February 2025 on booklet "Current"
When the user retrieves transactions for month February, year 2025, booklet "Current" with pageNumber 0 and pageSize 10
Then the system returns a paginated list of matching transactions for the requested page
And the response includes pagination metadata (pageNumber, pageSize, totalElements, totalPages)
And the balances (realSold, previsionalSold) are computed on all transactions for the period
```

### Scenario: Edit an existing transaction
```gherkin
Given an authenticated user who owns a booklet containing a transaction
When the user edits the transaction with a new amount or label
Then the updated transaction is persisted
And the system returns a TransactionResumeResult reflecting the new booklet balance
```

### Scenario: Find a transaction by ID
```gherkin
Given an authenticated user
When the user requests a transaction by its UUID
Then the system returns the matching Transaction
```

### Scenario: Delete multiple transactions
```gherkin
Given an authenticated user who owns a booklet with several transactions
When the user deletes transactions by their IDs for a given booklet
Then the transactions are removed
And the system returns a TransactionDeletionResult with deleted IDs and updated booklet amount
```

### Scenario: Confirm a preview (provisional) transaction
```gherkin
Given an authenticated user with a provisional transaction in a booklet
When the user confirms the preview transaction, optionally providing a new amount or date
Then the transaction is converted from preview to a real confirmed transaction
And the system returns a TransactionResumeResult with the updated balance
```

### Scenario: Confirm a virtual transaction and exclude its source month
```gherkin
Given an authenticated user viewing month M (e.g. May) on a booklet
And a virtual transaction exists for a regular transaction A in month M
When the user confirms the virtual transaction, optionally providing a new amount or date (possibly in a different month N)
Then a new real (isPreview = false) transaction is persisted with the provided date
And month M is marked as excluded in the tracker for regular transaction A
And the virtual transaction no longer appears for month M on subsequent loads
```

> **Rule**: when confirming a virtual transaction, the source month (the month from which the confirmation is triggered) must always be marked as excluded in the tracker — regardless of the final date chosen by the user. This prevents the virtual transaction from reappearing for the source month.

---

## Feature: Regular (Recurring) Transaction Management

As an authenticated user, I want to manage recurring transactions so that repeated income or expenses are automatically tracked.

### Scenario: Create a regular transaction linked to booklets
```gherkin
Given an authenticated user with one or more booklets
When the user books a regular transaction with a frequency, amount, label, and list of booklet IDs
Then the regular transaction is persisted and associated with the specified booklets
And the system returns the created RegularTransaction
```

### Scenario: List all regular transactions (paginated)
```gherkin
Given an authenticated user with existing regular transactions
When the user requests regular transactions with pageNumber 0 and pageSize 10
Then the system returns a paginated list of the user's regular transactions
And the response includes pagination metadata (pageNumber, pageSize, totalElements, totalPages)
```

### Scenario: Retrieve a regular transaction by ID
```gherkin
Given an authenticated user who owns a regular transaction
When the user requests it by its identifier
Then the system returns the matching RegularTransaction
```

### Scenario: Update a regular transaction
```gherkin
Given an authenticated user who owns a regular transaction
When the user updates its frequency, amount, or associated booklets
Then the changes are persisted
And the system returns the updated RegularTransaction
```

### Scenario: Delete a regular transaction
```gherkin
Given an authenticated user who owns a regular transaction
When the user deletes it by its identifier
Then the regular transaction is removed along with its generation trackers
```

### Scenario: Delete multiple regular transactions
```gherkin
Given an authenticated user with multiple regular transactions
When the user deletes several regular transactions by their identifiers
Then all specified regular transactions are removed
And the system returns the list of deleted identifiers
```

### Scenario: Link a booklet to a regular transaction
```gherkin
Given an authenticated user who owns a regular transaction and a booklet
When the user links the booklet to the regular transaction
Then the booklet is associated with the regular transaction
And future preview transactions will be generated for that booklet
```

### Scenario: Unlink a booklet from a regular transaction
```gherkin
Given an authenticated user with a regular transaction linked to a booklet
When the user unlinks the booklet from the regular transaction
Then the association is removed
And the generation tracker for that pair is deleted
And no more preview transactions will be generated for that booklet
```

---

## Feature: Tag Management

As an authenticated user, I want to manage tags so that I can categorize my transactions.

### Scenario: Create a custom tag
```gherkin
Given an authenticated user with no tag labelled "Groceries"
When the user creates a tag with label "Groceries"
Then the tag is persisted and returned
```

### Scenario: Create a tag with duplicate label
```gherkin
Given an authenticated user who already has a tag labelled "Groceries"
When the user creates another tag with label "Groceries"
Then the system returns a TAG_LABEL_ALREADY_TAKEN failure
```

### Scenario: List all tags
```gherkin
Given an authenticated user with custom and default tags
When the user requests all tags
Then the system returns the user's custom tags combined with application-wide default tags
```

### Scenario: Initialize default tags
```gherkin
Given no default tags exist in the system
When addDefaultTags is called
Then the predefined default tags are persisted
```

### Scenario: Initialize default tags idempotently
```gherkin
Given default tags already exist in the system
When addDefaultTags is called again
Then no duplicate tags are created
```

### Scenario: Delete a tag not in use
```gherkin
Given an authenticated user who owns a tag not assigned to any transaction
When the user deletes the tag
Then the tag is removed
```

### Scenario: Delete a tag in use without force
```gherkin
Given an authenticated user who owns a tag assigned to one or more transactions
When the user deletes the tag with force=false
Then the system returns a TAG_IN_USE failure
And the tag is not deleted
```

### Scenario: Force-delete a tag in use
```gherkin
Given an authenticated user who owns a tag assigned to transactions
When the user deletes the tag with force=true
Then all affected transactions and regular transactions are reassigned to the default tag
And the tag is deleted
```

### Scenario: Retrieve the default tag
```gherkin
Given default tags exist in the system
When the authenticated user requests the default tag
Then the system returns the global default tag
```

### Scenario: Edit a tag
```gherkin
Given an authenticated user who owns a custom (non-default) tag
When the user updates the tag's label to a new unique value
Then the updated tag is persisted and returned
```

### Scenario: Edit a default tag is rejected
```gherkin
Given a tag marked as default
When the user attempts to edit it
Then the system returns a failure
```

### Scenario: Create a sub-tag under a personal parent tag
```gherkin
Given an authenticated user who owns a personal tag "Food"
When the user creates a sub-tag "Restaurants" with parentId set to "Food"
Then the sub-tag is persisted with parentId pointing to "Food"
And the sub-tag is returned in the result
```

### Scenario: Sub-tag creation is rejected when parent does not exist
```gherkin
Given an authenticated user
When the user attempts to create a sub-tag with a non-existent parentId
Then the system returns a NOT_FOUND failure
```

### Scenario: Sub-tag creation is rejected under a default tag
```gherkin
Given a default tag "Alimentation & Restaurant"
When the user attempts to create a sub-tag with that tag as parent
Then the system returns an INVALID failure
```

### Scenario: Sub-tag creation is rejected when nesting depth exceeds 2
```gherkin
Given a sub-tag "Restaurants" that already has a parent tag
When the user attempts to create a sub-tag under "Restaurants"
Then the system returns an INVALID failure
```

### Scenario: Category distribution aggregates sub-tag amounts under parent
```gherkin
Given parent tag "Food" with sub-tags "Restaurants" (20 €) and "Groceries" (30 €) and a direct "Food" expense (10 €)
When the category distribution is computed
Then the "Food" entry has totalAmount 60 €
And its subCategories list contains entries for "Restaurants" and "Groceries"
```

### Scenario: Tags without sub-tags appear normally in distribution
```gherkin
Given a tag "Transport" with no sub-tags and a 15 € expense
When the category distribution is computed
Then "Transport" appears with totalAmount 15 €
And its subCategories list is empty
```

---

## Feature: Statistics & Reporting

As an authenticated user, I want to view statistics and reports so that I can analyze my financial activity.

### Scenario: Get monthly booklet statistics
```gherkin
Given an authenticated user who owns a booklet with transactions across several months
When the user requests monthly stats for a specific booklet and year
Then the system returns a MonthlyBookletStatsOutput with per-month aggregated data
```

### Scenario: Get category distribution
```gherkin
Given an authenticated user with categorized transactions
When the user requests category distribution, optionally filtered by booklet and date range
Then the system returns a CategoryDistributionOutput showing expense breakdown by tag/category
```

### Scenario: Get trend statistics
```gherkin
Given an authenticated user with historical transactions
When the user requests trend statistics, optionally filtered by booklet and date range
Then the system returns a TrendStatsOutput with aggregated trend data over time
```

### Scenario: Get daily trend statistics
```gherkin
Given an authenticated user with transactions in a given date range
When the user requests daily trend statistics with a start date and end date, optionally filtered by booklet
Then the system returns a DailyTrendStatsOutput containing one DailyTrend entry per day
And each entry includes the day's income, expenses, balance, and cumulative balance
```

### Scenario: Get daily trend statistics respecting custom monthly cycle
```gherkin
Given an authenticated user whose booklet has a monthly cycle from day 26 to day 25
When the user requests daily trend statistics for that custom range (e.g. 2024-12-26 to 2025-01-25)
Then the system returns daily entries spanning the full custom cycle
And cumulative balance is computed sequentially across the entire range
```

### Scenario: Get provisional transactions forecast
```gherkin
Given an authenticated user with regular transactions generating future previews
When the user requests provisional transactions for a date range
Then the system returns a PrevisionalTransactionsOutput with forecasted transactions
```

---

## Feature: CSV Import & Export

As an authenticated user, I want to import and export transactions via CSV so that I can bulk-manage my data.

### Scenario: Validate CSV content before import
```gherkin
Given an authenticated user who owns a booklet
When the user submits CSV content for validation with an optional month and year hint
Then the system parses the CSV and returns a CsvValidationReport with any warnings or errors
```

### Scenario: Import transactions from valid CSV
```gherkin
Given an authenticated user who owns a booklet
And the CSV content has been validated successfully
When the user imports transactions from the CSV content
Then the transactions are created in the specified booklet
And the system returns a CsvImportResult with created transactions and any line-level errors
```

### Scenario: Import transactions with validation skipped
```gherkin
Given an authenticated user who owns a booklet
When the user imports CSV content with skipValidation=true
Then the system skips the validation step and directly processes the import
```

### Scenario: Export transactions to CSV
```gherkin
Given an authenticated user with a list of non-preview transactions
When the user requests a CSV export
Then the system returns the transactions formatted as CSV content
And preview transactions are excluded from the output
```

---

## Feature: Data Retention Policy

As the system, I want to automatically purge personal data that has exceeded its configured
retention period so that JManager complies with GDPR Art. 5(1)(e) data minimisation.

### Scenario: Purge unconsented accounts past the retention threshold
```gherkin
Given a retention policy with unconsentedAccountRetentionDays = 30
And one or more user accounts with consent = null created more than 30 days ago
When the scheduled retention job runs
Then all expired unconsented accounts are permanently deleted
And a summary log entry records the number of deleted accounts
```

### Scenario: Recent unconsented accounts are not purged
```gherkin
Given a retention policy with unconsentedAccountRetentionDays = 30
And a user account with consent = null created less than 30 days ago
When the scheduled retention job runs
Then the account is NOT deleted
```

### Scenario: Consented accounts are never purged by the retention job
```gherkin
Given a user account with a non-null ConsentRecord regardless of age
When the scheduled retention job runs
Then the account is NOT deleted
```

---

## Feature: Administration

As an administrator, I want to manage users so that I can oversee the platform.

### Scenario: List users with pagination
```gherkin
Given an authenticated admin user
When the admin requests the user list with pageNumber 0 and pageSize 20
Then the system returns a Page of Users sorted by creation date (most recent first)
And the admin's own account is excluded from the results
```

### Scenario: Non-admin attempts to list users
```gherkin
Given an authenticated user without the ADMIN role
When the user attempts to list all users
Then the system returns an authorization failure
```

---

## Feature: Feature Flags

As the system, I want global, runtime-controlled feature flags so that behaviour can be toggled without
redeploying. Flags are global ON/OFF (no per-user/per-plan targeting at flag level), persisted in the database,
and exposed to the frontend via a dedicated endpoint.

### Scenario: Resolve a flag state
```gherkin
Given a feature flag "email-verification" persisted with enabled = true
When the system checks whether "email-verification" is enabled
Then the service returns true
```

### Scenario: Unknown flag defaults to disabled
```gherkin
Given no persisted state exists for a flag key
When the system checks whether it is enabled
Then the service returns false
```

### Scenario: Administrator toggles a flag at runtime
```gherkin
Given an authenticated admin user
And a flag "email-verification" currently disabled
When the admin toggles it to enabled = true
Then subsequent reads report it as enabled
```

### Scenario: Frontend reads all flags
```gherkin
Given several known flags with mixed states
When the client calls GET /feature-flags
Then it receives a map of every known flag key to its boolean state
```

---

## Feature: Email Verification

As the system, I want to verify that self-registered users own a real mailbox.
Admin-created users are implicitly trusted and marked as verified from creation.

### Scenario: Self-registered user must verify their email
```gherkin
Given a visitor completes the registration form
When the account is created
Then emailVerified = false
And a verification email with a unique token is sent
```

### Scenario: Admin-created user is auto-verified
```gherkin
Given an admin creates a user through the admin interface
When the account is created
Then emailVerified = true
And no verification email is sent
```

### Scenario: Valid token confirms the email
```gherkin
Given a self-registered user with a valid, unexpired verification token
When the user follows the link in the verification email
Then emailVerified becomes true
And all outstanding tokens for that user are cleared
```

### Scenario: Expired or unknown token is rejected
```gherkin
Given a verification token that is expired or unknown
When the user follows the link
Then the system returns a failure
And emailVerified remains false
```

### Scenario: Unverified user sees a warning indicator on the Paramètres tab
```gherkin
Given an authenticated user with emailVerified = false
When any authenticated page renders
Then the "Paramètres" tab shows a warning indicator with tooltip "Votre compte doit être vérifié"
```

### Scenario: Unverified user can request a new verification email from settings
```gherkin
Given an authenticated user with emailVerified = false on the settings page
When the user clicks "Vérifier mon e-mail"
Then a new verification email is dispatched
```
