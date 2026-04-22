import type { AppTableColumn } from '../../components/AppTable.vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppTable from '../../components/AppTable.vue'

// ── Stubs ─────────────────────────────────────────────────────────────────────

const DataTableStub = {
  name: 'DataTable',
  props: ['value', 'dataKey', 'rowClass', 'selectionMode', 'selection', 'metaKeySelection', 'scrollable', 'loading'],
  emits: ['update:selection', 'row-dblclick'],
  methods: {
    handleRowClick(row: Record<string, unknown>) {
      // @ts-expect-error – options-API `this` inside stub
      if (this.selectionMode !== 'multiple') return
      // eslint-disable-next-line ts/ban-ts-comment
      // @ts-expect-error
      const current: unknown[] = this.selection ? [...this.selection] : []
      const idx = current.indexOf(row)
      if (idx === -1) current.push(row)
      else current.splice(idx, 1)
      // eslint-disable-next-line ts/ban-ts-comment
      // @ts-expect-error
      this.$emit('update:selection', current)
    },
    handleRowDblclick(row: Record<string, unknown>) {
      // eslint-disable-next-line ts/ban-ts-comment
      // @ts-expect-error
      this.$emit('row-dblclick', { data: row })
    },
  },
  template: `
    <div data-stub="datatable" :data-selection-mode="selectionMode">
      <slot />
      <template v-if="!value || value.length === 0">
        <slot name="empty" />
      </template>
      <div
        v-for="(row, idx) in value"
        :key="idx"
        data-stub="row"
        :class="rowClass ? rowClass(row) : ''"
        @click="handleRowClick(row)"
        @dblclick="handleRowDblclick(row)"
      />
    </div>
  `,
}

const ColumnStub = {
  name: 'Column',
  props: ['field', 'header', 'sortable', 'style', 'selectionMode', 'frozen', 'alignFrozen'],
  template: `
    <div data-stub="column" :data-field="field" :data-header="header" :data-selection-mode="selectionMode">
      <div data-stub="column-header"><slot name="header" /></div>
      <div data-stub="column-body"><slot name="body" :data="{ id: 'stub-data' }" /></div>
    </div>
  `,
}

const globalStubs = { DataTable: DataTableStub, Column: ColumnStub }

// ── Fixtures ──────────────────────────────────────────────────────────────────

const sampleColumns: AppTableColumn[] = [
  { field: 'name', header: 'Nom', sortable: true },
  { field: 'age', header: 'Âge' },
]

const sampleRows = [
  { name: 'Alice', age: 30 },
  { name: 'Bob', age: 25 },
]

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('components/AppTable', () => {
  describe('renders headers and rows from props', () => {
    it('renders one Column per column definition', () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
      })

      const columns = wrapper.findAll('[data-stub="column"]')
      expect(columns).toHaveLength(sampleColumns.length)
      // eslint-disable-next-line ts/ban-ts-comment
      // @ts-expect-error
      expect(columns[0].attributes('data-header')).toBe('Nom')
      // eslint-disable-next-line ts/ban-ts-comment
      // @ts-expect-error
      expect(columns[1].attributes('data-header')).toBe('Âge')
    })

    it('renders one row per item in the rows list', () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
      })

      expect(wrapper.findAll('[data-stub="row"]')).toHaveLength(sampleRows.length)
    })
  })

  describe('empty state', () => {
    it('renders the empty slot when rows list is empty', () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: [], dataKey: 'name' },
        global: { stubs: globalStubs },
        slots: { empty: '<div data-test="empty-state">No data</div>' },
      })

      expect(wrapper.find('[data-test="empty-state"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="empty-state"]').text()).toBe('No data')
    })

    it('does not render the empty slot when rows list is non-empty', () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
        slots: { empty: '<div data-test="empty-state">No data</div>' },
      })

      expect(wrapper.find('[data-test="empty-state"]').exists()).toBe(false)
    })
  })

  describe('row selection', () => {
    it('passes selectionMode="multiple" to DataTable when selectable is true', () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name', selectable: true },
        global: { stubs: globalStubs },
      })

      expect(wrapper.find('[data-stub="datatable"]').attributes('data-selection-mode')).toBe('multiple')
    })

    it('emits update:selection with the clicked row when selectable is true', async () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name', selectable: true },
        global: { stubs: globalStubs },
      })

      await wrapper.findAll('[data-stub="row"]')[0].trigger('click')

      expect(wrapper.emitted('update:selection')).toBeTruthy()
      expect((wrapper.emitted('update:selection')![0][0] as unknown[]).length).toBe(1)
    })

    it('removes the row from selection when clicked a second time', async () => {
      const wrapper = mount(AppTable, {
        props: {
          columns: sampleColumns,
          rows: sampleRows,
          dataKey: 'name',
          selectable: true,
          selection: [sampleRows[0]],
        },
        global: { stubs: globalStubs },
      })

      await wrapper.findAll('[data-stub="row"]')[0].trigger('click')

      const emissions = wrapper.emitted('update:selection')!
      const lastEmission = emissions.at(-1)[0] as unknown[]
      expect(lastEmission).not.toContain(sampleRows[0])
    })

    it('does not emit update:selection when selectable is false', async () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name', selectable: false },
        global: { stubs: globalStubs },
      })

      await wrapper.findAll('[data-stub="row"]')[0].trigger('click')

      expect(wrapper.emitted('update:selection')).toBeFalsy()
    })

    it('does not render selection column when selectable is false', () => {
      const columnsWithSelection: AppTableColumn[] = [
        { selectionMode: 'multiple', style: { width: '3rem' } },
        { field: 'name', header: 'Nom' },
      ]

      const wrapper = mount(AppTable, {
        props: { columns: columnsWithSelection, rows: sampleRows, dataKey: 'name', selectable: false },
        global: { stubs: globalStubs },
      })

      const selectionCols = wrapper.findAll('[data-stub="column"][data-selection-mode="multiple"]')
      expect(selectionCols).toHaveLength(0)
    })

    it('renders the selection column when selectable is true', () => {
      const columnsWithSelection: AppTableColumn[] = [
        { selectionMode: 'multiple', style: { width: '3rem' } },
        { field: 'name', header: 'Nom' },
      ]

      const wrapper = mount(AppTable, {
        props: { columns: columnsWithSelection, rows: sampleRows, dataKey: 'name', selectable: true },
        global: { stubs: globalStubs },
      })

      const selectionCols = wrapper.findAll('[data-stub="column"][data-selection-mode="multiple"]')
      expect(selectionCols).toHaveLength(1)
    })
  })

  describe('row double-click', () => {
    it('emits row-dblclick with row data when a row is double-clicked', async () => {
      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
      })

      await wrapper.findAll('[data-stub="row"]')[1].trigger('dblclick')

      expect(wrapper.emitted('row-dblclick')).toBeTruthy()
      expect((wrapper.emitted('row-dblclick')![0][0] as { data: unknown }).data).toEqual(sampleRows[1])
    })
  })

  describe('custom column slots', () => {
    it('renders body-{slotName} slot content inside the column body', () => {
      const columnsWithSlot: AppTableColumn[] = [
        { field: 'name', header: 'Nom', slotName: 'name' },
      ]

      const wrapper = mount(AppTable, {
        props: { columns: columnsWithSlot, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
        slots: { 'body-name': '<span data-test="custom-body">custom</span>' },
      })

      expect(wrapper.find('[data-test="custom-body"]').exists()).toBe(true)
    })

    it('renders header-{headerSlotName} slot content inside the column header', () => {
      const columnsWithHeaderSlot: AppTableColumn[] = [
        { field: 'name', header: 'Nom', headerSlotName: 'nameFilter' },
      ]

      const wrapper = mount(AppTable, {
        props: { columns: columnsWithHeaderSlot, rows: sampleRows, dataKey: 'name' },
        global: { stubs: globalStubs },
        slots: { 'header-nameFilter': '<div data-test="custom-header">Filter</div>' },
      })

      expect(wrapper.find('[data-test="custom-header"]').exists()).toBe(true)
    })
  })

  describe('row class', () => {
    it('applies the CSS class returned by rowClass to the matching row', () => {
      const rowClassFn = (row: Record<string, unknown>) =>
        row.name === 'Alice' ? 'highlight' : ''

      const wrapper = mount(AppTable, {
        props: { columns: sampleColumns, rows: sampleRows, dataKey: 'name', rowClass: rowClassFn },
        global: { stubs: globalStubs },
      })

      const rows = wrapper.findAll('[data-stub="row"]')
      expect(rows[0].classes()).toContain('highlight')
      expect(rows[1].classes()).not.toContain('highlight')
    })
  })
})
