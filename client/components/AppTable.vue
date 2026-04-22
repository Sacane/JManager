<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed } from 'vue'

export interface AppTableColumn {
  field?: string
  header?: string
  sortable?: boolean
  style?: string | Record<string, string>
  headerStyle?: string | Record<string, string>
  slotName?: string
  headerSlotName?: string
  selectionMode?: 'multiple'
  frozen?: boolean
  alignFrozen?: 'left' | 'right'
  exportable?: boolean
}

const props = withDefaults(defineProps<{
  columns: AppTableColumn[]
  rows: T[]
  dataKey: string
  selectable?: boolean
  rowClass?: (row: T) => string
  scrollable?: boolean
  scrollHeight?: string
  loading?: boolean
}>(), {
  selectable: false,
  rowClass: undefined,
  scrollable: false,
  scrollHeight: 'flex',
  loading: false,
})

const emit = defineEmits<{
  'row-dblclick': [event: { data: T, index?: number, originalEvent?: Event }]
  'update:selection': [value: T[]]
}>()

const selection = defineModel<T[]>('selection', { default: () => [] })

const visibleColumns = computed(() =>
  props.selectable
    ? props.columns
    : props.columns.filter(col => !col.selectionMode),
)
</script>

<template>
  <DataTable
    v-model:selection="selection"
    :value="rows"
    :data-key="dataKey"
    :row-class="rowClass"
    :selection-mode="selectable ? 'multiple' : undefined"
    :meta-key-selection="false"
    :scrollable="scrollable"
    :scroll-height="scrollable ? scrollHeight : undefined"
    :loading="loading"
    class="app-table"
    @row-dblclick="(event: any) => emit('row-dblclick', event)"
  >
    <template #empty>
      <slot name="empty" />
    </template>

    <template v-if="$slots.loading" #loading>
      <slot name="loading" />
    </template>

    <Column
      v-for="(col, idx) in visibleColumns"
      :key="col.field ?? col.slotName ?? col.headerSlotName ?? col.selectionMode ?? idx"
      :field="col.field"
      :header="col.header"
      :sortable="col.sortable"
      :style="col.style"
      :header-style="col.headerStyle"
      :selection-mode="col.selectionMode"
      :frozen="col.frozen"
      :align-frozen="col.alignFrozen"
      :exportable="col.exportable"
    >
      <template v-if="col.headerSlotName" #header>
        <slot :name="`header-${col.headerSlotName}`" />
      </template>
      <template v-if="col.slotName" #body="slotProps">
        <slot :name="`body-${col.slotName}`" v-bind="slotProps" />
      </template>
    </Column>
  </DataTable>
</template>

<style scoped>
.app-table :deep(.p-datatable-thead > tr > th) {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff;
  font-weight: 700;
  padding: 16px;
  border: none;
  text-transform: uppercase;
  font-size: 0.875rem;
  letter-spacing: 0.05em;
}

.app-table :deep(.p-datatable-tbody > tr) {
  transition: all 0.2s ease;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-bg);
}

.app-table :deep(.p-datatable-tbody > tr:nth-child(even)) {
  background: rgba(130, 42, 204, 0.04);
}

.app-table :deep(.p-datatable-tbody > tr:hover) {
  background: var(--card-hover-bg) !important;
}

.app-table :deep(.p-datatable-tbody > tr > td) {
  padding: 16px;
  border: none;
  color: var(--text-primary);
}

.app-table :deep(.p-button.p-button-text.p-button-rounded) {
  width: 2.5rem;
  height: 2.5rem;
  transition: all 0.2s ease;
  border: 2px solid currentColor;
  border-radius: 50%;
  opacity: 0.6;
}

.app-table :deep(.p-button.p-button-text.p-button-rounded:hover) {
  transform: scale(1.1);
  opacity: 1;
  box-shadow: 0 2px 8px currentColor;
}

.app-table :deep(.p-button.p-button-text.p-button-rounded.text-\[var\(--primary\)\]) {
  border-color: rgba(130, 42, 204, 0.4);
}

.app-table :deep(.p-button.p-button-text.p-button-rounded.text-\[var\(--primary\)\]:hover) {
  border-color: rgba(130, 42, 204, 0.8);
  box-shadow: 0 2px 8px rgba(130, 42, 204, 0.3);
}

.app-table :deep(.p-button.p-button-text.p-button-rounded.text-emerald-500) {
  border-color: rgba(16, 185, 129, 0.4);
}

.app-table :deep(.p-button.p-button-text.p-button-rounded.text-emerald-500:hover) {
  border-color: rgba(16, 185, 129, 0.8);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

/* Preview row amber highlight */
.app-table :deep(.preview-row) {
  background: rgba(245, 158, 11, 0.1) !important;
  box-shadow: inset 3px 0 0 rgba(245, 158, 11, 0.5);
}

.app-table :deep(.preview-row:nth-child(even)) {
  background: rgba(245, 158, 11, 0.16) !important;
}

.app-table :deep(.preview-row:hover) {
  background: rgba(245, 158, 11, 0.22) !important;
}
</style>
