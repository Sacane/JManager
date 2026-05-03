# Sub-Tags — Application Module

**Context**
Once the domain and infrastructure layers support sub-tags, the application (REST API) layer must
expose:
1. A dedicated endpoint for creating a sub-tag under a parent personal tag.
2. An updated tag listing contract that includes the `parentId` field so the client can reconstruct
   the hierarchy.
3. An updated category-distribution response that includes a `subCategories` breakdown per parent tag,
   enabling the frontend to render a secondary pie chart.

**Acceptance Criteria**

Feature: Sub-tag REST API

    Scenario: 1 — Create a sub-tag successfully
        Given an authenticated user who owns a personal tag with id <parentId>
        When the user calls POST /tag/sub-tag with body { tagLabel, colorDTO, parentId }
        Then the response status is 201 Created
        And the response body contains the new sub-tag with its parentId set to <parentId>

    Scenario: 2 — Create a sub-tag with a non-existent parent returns 404
        Given an authenticated user
        When the user calls POST /tag/sub-tag with a parentId that does not match any existing tag
        Then the response status is 404 Not Found

    Scenario: 3 — Create a sub-tag under a default tag returns 400
        Given an authenticated user
        When the user calls POST /tag/sub-tag with a parentId pointing to a default tag
        Then the response status is 400 Bad Request

    Scenario: 4 — Create a sub-tag under another sub-tag returns 400
        Given an authenticated user who owns sub-tag "Restaurants" (which itself has a parent)
        When the user calls POST /tag/sub-tag with parentId = Restaurants.id
        Then the response status is 400 Bad Request

    Scenario: 5 — GET /tag returns all tags with their parentId populated
        Given an authenticated user who owns tag "Food" (id = X) and sub-tag "Restaurants" (parentId = X)
        When the user calls GET /tag
        Then the response body contains both tags
        And the "Restaurants" entry has parentId = X
        And the "Food" entry has parentId = null

    Scenario: 6 — GET /stats/category-distribution includes sub-category breakdown
        Given a parent tag "Food" with sub-tags "Restaurants" (20 €) and "Groceries" (30 €)
        And a direct expense on "Food" (10 €)
        When the user calls GET /stats/category-distribution
        Then the "Food" category entry has totalAmount = 60 €
        And its subCategories list contains entries for "Restaurants" (20 €) and "Groceries" (30 €)

    Scenario: 7 — GET /stats/category-distribution returns empty subCategories for leaf tags
        Given a tag "Transport" with no sub-tags and a 15 € expense
        When the user calls GET /stats/category-distribution
        Then the "Transport" entry has totalAmount = 15 €
        And its subCategories list is empty

**Notes**
- New endpoint: `POST /tag/sub-tag`, accepting a `CreateSubTagRequest(tagLabel: String, colorDTO: ColorDTO, parentId: UUID)`.
- `TagDTO` must be extended with `val parentId: UUID? = null`.
- `CategoryDataDTO` must be extended with `val subCategories: List<CategoryDataDTO> = emptyList()`.
- The `CategoryData.toDTO()` mapper must recursively map sub-categories.
- Application-layer API tests must cover all HTTP status codes listed in scenarios 1–7.
- No domain logic must be duplicated in the application layer; the controller must delegate
  entirely to `CreateSubTagUseCase`.
