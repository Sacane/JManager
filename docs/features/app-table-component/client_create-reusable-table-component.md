# Create Reusable Table Component : Client Module

**Contexte**
The application currently duplicates PrimeVue `DataTable` configurations across multiple pages (`booklet/[id].vue`, `admin/index.vue`, `regular-transaction/index.vue`), each with their own local CSS overrides (zebra striping, hover effects, preview-row highlight, etc.). A shared `AppTable` component should encapsulate this repeated logic and enforce a consistent look and feel across the application.

The reference implementation is `client/pages/booklet/[id].vue`, which is the most complete usage: multi-row selection, sortable columns, dynamic row classes, custom column headers (tag filter dropdown), and per-column body slots.

**Critères d'acceptation**

Feature: Reusable AppTable component
  In order to avoid duplicating DataTable setup and styling across pages
  As a developer
  I want a generic AppTable component that wraps PrimeVue DataTable with the application design system

  Scenario: Renders headers and rows from props
    Given a list of column definitions and a non-empty list of rows
    When the AppTable component is mounted
    Then it renders one header cell per column definition
    And it renders one row per item in the rows list

  Scenario: Renders empty state when rows list is empty
    Given a list of column definitions and an empty rows list
    When the AppTable component is mounted
    Then the content of the empty slot is displayed instead of a table body

  Scenario: Row selection toggles on click when selectable is true
    Given an AppTable with selectable set to true and a non-empty rows list
    When the user clicks a row
    Then the row is added to the selection and an update:selection event is emitted
    When the user clicks the same row again
    Then the row is removed from the selection and an update:selection event is emitted

  Scenario: Row double-click emits row-dblclick event
    Given an AppTable with a non-empty rows list
    When the user double-clicks a row
    Then the component emits a row-dblclick event with the row data as payload

  Scenario: Custom column body rendered via named slot
    Given a column definition with a slotName value
    When the parent provides a slot named body-{slotName}
    Then the slot content is rendered inside the column body cell for each row

  Scenario: Custom column header rendered via named slot
    Given a column definition with a headerSlotName value
    When the parent provides a slot named header-{headerSlotName}
    Then the slot content is rendered inside the column header cell

  Scenario: Dynamic row class applied via rowClass prop
    Given an AppTable with a rowClass function prop
    When the component renders each row
    Then the CSS class returned by rowClass for that row is applied to the row element

  Scenario: Per-row action column rendered via named slot
    Given a column definition with slotName set to 'actions'
    When the parent provides a slot named body-actions with row data exposed
    Then the slot content (e.g. edit, delete, confirm buttons) is rendered inside the actions cell for each row
    And the slot receives the row data object as a scoped slot parameter

  Scenario: Parent handles global bulk actions via selection event
    Given an AppTable with selectable set to true and several rows selected
    When the parent listens to update:selection events
    Then the parent receives the current selection array on every change
    And can use it to drive global action buttons (e.g. bulk delete, export) outside the table

  Scenario: Selection is disabled when selectable is false or omitted
    Given an AppTable with selectable set to false
    When the component is mounted
    Then no selection column is rendered
    And clicking a row does not emit any update:selection event

**Notes**
- Component file: `client/components/AppTable.vue`
- TypeScript interface to define alongside the component:
  ```ts
  interface AppTableColumn {
    field?: string           // data field used for sorting
    header?: string          // static header label
    sortable?: boolean       // enables column sort
    style?: string | Record<string, string>
    slotName?: string        // enables #body-{slotName} slot on the parent
    headerSlotName?: string  // enables #header-{headerSlotName} slot on the parent
    selectionMode?: 'multiple' // marks this column as the checkbox selection column
  }
  ```
- Props: `columns: AppTableColumn[]`, `rows: T[]`, `dataKey: string`, `selectable?: boolean`, `rowClass?: (row: T) => string`
- Events: `row-dblclick`, `update:selection`
- Slots: `#empty`, `#body-{slotName}`, `#header-{headerSlotName}`
- **Per-row actions pattern**: declare a column with `slotName: 'actions'` and provide a `#body-actions="{ data }"` slot in the parent. The slot receives the full row object, allowing each page to render its own set of action buttons (edit, delete, confirm preview, etc.) without coupling the table component to any specific action logic.
- **Global / bulk actions pattern**: the parent owns the action toolbar (positioned outside `AppTable`). It drives it by binding `v-model:selection` on `AppTable` and reacting to `update:selection` events to enable/disable bulk action buttons (e.g. delete selection, export CSV). The table component has no knowledge of these actions.
- Must bundle all DataTable `:deep()` CSS overrides currently duplicated in `booklet/[id].vue`, `admin/index.vue` and `regular-transaction/index.vue` (zebra striping, row hover, preview-row amber highlight, action button styles)
- After the component is stable, migrate the following pages to use `AppTable`:
  - `booklet/[id].vue`
  - `admin/index.vue`
  - `regular-transaction/index.vue`
- Vitest tests should cover: correct header rendering, empty state slot, selection toggle, row-dblclick event, slotName body slot, and rowClass application
