# Sub-Tags — Infrastructure Module

**Context**
The domain layer introduces a parent-child relationship on `Tag.Personal` (see domain issue).
The infrastructure layer must persist this relationship using a self-referencing foreign key on the
`tag_personal` table, update the JPA entity accordingly, and expose the new repository operations
required by the domain port.

**Acceptance Criteria**

Feature: Sub-tag persistence

    Scenario: 1 — A sub-tag is persisted with its parent reference
        Given a personal tag "Food" already stored in the database
        When a sub-tag "Restaurants" is saved with parentId = Food.id
        Then the "Restaurants" row in tag_personal has parent_id = Food.id
        And the row can be retrieved with its parent reference intact

    Scenario: 2 — Tags without a parent are persisted with a null parent_id
        Given a personal tag "Transport" with no parent
        When the tag is saved
        Then the "Transport" row in tag_personal has parent_id = NULL

    Scenario: 3 — findSubTagsByParentId returns all direct children
        Given a parent tag "Food" with sub-tags "Restaurants" and "Groceries" in the database
        When findSubTagsByParentId is called with Food.id
        Then the result contains exactly "Restaurants" and "Groceries"

    Scenario: 4 — hasSubTags returns true when at least one child exists
        Given a parent tag "Food" with at least one sub-tag stored
        When hasSubTags is called with Food.id
        Then the result is true

    Scenario: 5 — hasSubTags returns false when no children exist
        Given a personal tag "Transport" with no sub-tags stored
        When hasSubTags is called with Transport.id
        Then the result is false

    Scenario: 6 — Deleting a parent tag cascades to its sub-tags
        Given a parent tag "Food" and its sub-tag "Restaurants" in the database
        When the parent tag is deleted (force=true path)
        Then both "Food" and "Restaurants" rows are removed from tag_personal

    Scenario: 7 — The getAllDefault query returns sub-tags with their parentId populated
        Given a parent tag "Food" (id = X) and a sub-tag "Restaurants" (parentId = X)
        When getAllDefault is called for the owning user
        Then the returned "Restaurants" domain object has parentId = X

**Notes**
- A Flyway migration must add `parent_id UUID REFERENCES tag_personal(id_tag) ON DELETE CASCADE`
  to the `tag_personal` table. The column must be nullable to remain backward-compatible.
- `TagPersonalResource` must gain a nullable `@ManyToOne(optional = true) @JoinColumn(name = "parent_id")`
  self-reference field.
- `TagPersonalPostgresRepository` must be extended with:
    - `fun findAllByParentId(parentId: UUID): List<TagPersonalResource>`
    - `fun existsByParentId(parentId: UUID): Boolean`
- `TagRepositoryJpaAdapter` must implement the two new `TagRepository` methods:
    `findSubTagsByParentId` and `hasSubTags`.
- The `toDomain()` mapper for `TagPersonalResource` must propagate `parent?.idTag` as `parentId`
  in the resulting `Tag.Personal`.
- Persistence tests must use the real Postgres test container (not the in-memory fake).
