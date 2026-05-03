# Sub-Tags — Domain Module

**Context**
The tag domain model currently supports two tag variants: `Tag.Default` and `Tag.Personal`.
Neither supports a hierarchical parent-child relationship.
This issue introduces the sub-tag concept at the domain level: a `Tag.Personal` can optionally reference
a parent `Tag.Personal` via a `parentId: UUID?`, enabling two-level categorisation of transactions.

The domain must enforce the following business rules:
- Only `Tag.Personal` instances can be sub-tags (default tags cannot have a parent or be used as parent).
- Sub-tags may not themselves have sub-tags (the hierarchy is strictly limited to two levels).
- A sub-tag parent must exist and belong to the requesting user before the sub-tag is created.
- When computing the category distribution (pie chart), each parent tag aggregates the amounts of all
  its sub-tags. The `CategoryData` output must expose the sub-category breakdown alongside the aggregate.

**Acceptance Criteria**

Feature: Sub-tag domain model and business rules

    Scenario: 1 — Create a sub-tag under an existing personal parent tag
        Given an authenticated user who owns a personal tag "Food" with id <parentId>
        When the user creates a sub-tag "Restaurants" with parentId <parentId>
        Then the sub-tag is persisted with parentId set to <parentId>
        And the sub-tag is returned in the result

    Scenario: 2 — Creating a sub-tag under a non-existent parent fails
        Given an authenticated user
        When the user attempts to create a sub-tag with a parentId that does not exist
        Then the system returns a NOT_FOUND failure
        And the sub-tag is not persisted

    Scenario: 3 — Creating a sub-tag under a default tag is rejected
        Given a default tag "Alimentation & Restaurant"
        When the user attempts to create a sub-tag with that default tag as parent
        Then the system returns an INVALID failure
        And the sub-tag is not persisted

    Scenario: 4 — Creating a sub-tag under an existing sub-tag is rejected (max depth = 2)
        Given a personal tag "Food" and a sub-tag "Restaurants" whose parentId points to "Food"
        When the user attempts to create a sub-tag under "Restaurants"
        Then the system returns an INVALID failure with key "domain.tag.create_sub_tag.parent_is_subtag"
        And the sub-tag is not persisted

    Scenario: 5 — Category distribution aggregates sub-tag amounts under the parent tag
        Given a parent tag "Food" with sub-tags "Restaurants" and "Groceries"
        And three expense transactions: one tagged "Restaurants" (20 €), one tagged "Groceries" (30 €),
            one tagged "Food" directly (10 €)
        When the category distribution is calculated
        Then the result contains one entry for "Food"
        And its totalAmount equals 60 € (sum of all direct and indirect transactions)
        And its subCategories list contains entries for "Restaurants" (20 €) and "Groceries" (30 €)

    Scenario: 6 — Tags without sub-tags appear normally in the distribution
        Given a personal tag "Transport" with no sub-tags
        And an expense transaction tagged "Transport" (15 €)
        When the category distribution is calculated
        Then the result contains one entry for "Transport" with totalAmount 15 €
        And its subCategories list is empty

    Scenario: 7 — Deleting a parent tag with force also deletes its sub-tags
        Given a parent tag "Food" with sub-tags "Restaurants" and "Groceries"
        When the user deletes "Food" with force=true
        Then "Food", "Restaurants", and "Groceries" are all removed
        And all affected transactions are reassigned to the default tag

    Scenario: 8 — Deleting a parent tag in use without force is rejected
        Given a parent tag "Food" with sub-tags "Restaurants" assigned to a transaction
        When the user deletes "Food" with force=false
        Then the system returns a TAG_IN_USE failure
        And no tags are deleted

    Scenario: 9 — GetAllTagsUseCase returns tags with their parentId
        Given an authenticated user with a parent tag "Food" and a sub-tag "Restaurants" (parentId = Food.id)
        When the user requests all tags
        Then "Restaurants" is returned with its parentId set to Food.id
        And "Food" is returned with no parentId

**Notes**
- `Tag.Personal` must be extended with `val parentId: UUID? = null`.
- `TagRepository` must be extended with:
    - `findSubTagsByParentId(parentId: UUID): List<Tag.Personal>`
    - `hasSubTags(tagId: UUID): Boolean`
- A new `CreateSubTagUseCase` (with its command) must be introduced; it must NOT reuse `AddTagUseCase`
  because the validation rules differ (parent existence check, depth check).
- `CategoryData` must gain a `subCategories: List<CategoryData>` field (empty list for tags with no sub-tags).
- `CategoryDistributionCalculatorImpl` must be updated to group sub-tag transactions under their parent,
  computing the aggregate total and populating the `subCategories` list.
- Domain tests must use `InMemoryTagRepository` exclusively — no infrastructure or Spring dependency.
- The `DeleteTagUseCase` must be updated to cascade to sub-tags when `force=true`.
