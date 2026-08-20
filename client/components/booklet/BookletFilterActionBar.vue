<script setup lang="ts">
interface TagFilterOption {
  label: string
  value: string
}

interface Props {
  isMobile: boolean
  hideActionButtons?: boolean
  globalFilter: 'none' | 'all' | 'preview'
  transactionsCount: number
  previewTransactionsCount: number
  isGlobalFilterLoading?: boolean
  hasSelection: boolean
  selectedCount: number
  selectedAmount: number
  selectedAmountLabel: string
  hasRegenerableTransactions: boolean
  isAnyActionLoading: boolean
  isDeleteLoading: boolean
  isExportCsvLoading: boolean
  isRegenerateLoading: boolean
  tagFilterOptions?: TagFilterOption[]
  subTagFilterOptions?: TagFilterOption[]
  selectedTagFilter?: string
  selectedSubTagFilter?: string
}

const props = withDefaults(defineProps<Props>(), {
  hideActionButtons: false,
  isGlobalFilterLoading: false,
  tagFilterOptions: () => [],
  subTagFilterOptions: () => [],
  selectedTagFilter: '',
  selectedSubTagFilter: '',
})

const emit = defineEmits<{
  'update:globalFilter': [val: 'none' | 'all' | 'preview']
  'update:selectedTagFilter': [val: string]
  'update:selectedSubTagFilter': [val: string]
  'newTransaction': []
  'newPreview': []
  'importCsv': []
  'exportCsv': []
  'regenerate': []
  'delete': []
}>()
</script>

<template>
  <!-- Filtres + Actions -->
  <div v-if="isMobile || !hideActionButtons" class="flex items-center gap-2 mb-2 overflow-x-auto">
    <!-- Mobile: filter chips -->
    <template v-if="isMobile">
      <!-- Global filters -->
      <button
        class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap shrink-0 border"
        :class="globalFilter === 'all'
          ? 'border-[var(--primary)] text-[var(--primary)] bg-[rgba(101,8,204,0.07)]'
          : 'border-[var(--card-border)] text-[var(--text-secondary)]'"
        :disabled="isGlobalFilterLoading"
        aria-label="Tout le mois"
        @click="emit('update:globalFilter', globalFilter === 'all' ? 'none' : 'all')"
      >
        <i :class="isGlobalFilterLoading && globalFilter === 'all' ? 'pi pi-spin pi-spinner text-xs' : 'pi pi-globe text-xs'" />
        <span>Tout</span>
      </button>
      <button
        class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap shrink-0 border"
        :class="globalFilter === 'preview'
          ? 'border-amber-500 text-amber-600 bg-amber-500/8'
          : 'border-[var(--card-border)] text-[var(--text-secondary)]'"
        :disabled="isGlobalFilterLoading"
        aria-label="Prévisionnelles du mois"
        @click="emit('update:globalFilter', globalFilter === 'preview' ? 'none' : 'preview')"
      >
        <i :class="isGlobalFilterLoading && globalFilter === 'preview' ? 'pi pi-spin pi-spinner text-xs' : 'pi pi-calendar text-xs'" />
        <span>Prév.</span>
      </button>

      <!-- Tag filters -->
      <template v-if="props.tagFilterOptions && props.tagFilterOptions.length > 1">
        <div class="w-px h-5 bg-[var(--border-color)] mx-0.5 shrink-0" />
        <Select
          :model-value="selectedTagFilter"
          :options="tagFilterOptions"
          option-label="label"
          option-value="value"
          class="!w-[90px] !h-9 shrink-0"
          @update:model-value="(val: string) => emit('update:selectedTagFilter', val)"
        >
          <template #value="{ value: val }">
            <span class="text-xs font-semibold truncate block" :class="val ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]'">
              {{ val ? (tagFilterOptions?.find(o => o.value === val)?.label ?? val) : 'Tag' }}
            </span>
          </template>
          <template #option="{ option }">
            <span class="text-xs">{{ option.label || 'Tous' }}</span>
          </template>
        </Select>
      </template>
      <template v-if="props.subTagFilterOptions && props.subTagFilterOptions.length > 1">
        <Select
          :model-value="selectedSubTagFilter"
          :options="subTagFilterOptions"
          option-label="label"
          option-value="value"
          class="!w-[90px] !h-9 shrink-0"
          @update:model-value="(val: string) => emit('update:selectedSubTagFilter', val)"
        >
          <template #value="{ value: val }">
            <span class="text-xs font-semibold truncate block" :class="val ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]'">
              {{ val ? (subTagFilterOptions?.find(o => o.value === val)?.label ?? val) : 'S-tag' }}
            </span>
          </template>
          <template #option="{ option }">
            <span class="text-xs">{{ option.label || 'Tous' }}</span>
          </template>
        </Select>
      </template>
    </template>

    <!-- Desktop: filter buttons + action buttons -->
    <template v-if="!isMobile">
      <!-- Global: all month -->
      <button
        class="px-2.5 py-1 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0 border"
        :class="globalFilter === 'all'
          ? 'border-[var(--primary)] text-[var(--primary)] bg-[rgba(101,8,204,0.07)]'
          : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-[var(--primary)] hover:text-[var(--primary)]'"
        :disabled="isGlobalFilterLoading"
        @click="emit('update:globalFilter', globalFilter === 'all' ? 'none' : 'all')"
      >
        <i class="mr-1.5" :class="isGlobalFilterLoading && globalFilter === 'all' ? 'pi pi-spin pi-spinner' : 'pi pi-globe'" />
        Tout le mois
      </button>

      <!-- Global: previews month -->
      <button
        class="px-2.5 py-1 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0 border"
        :class="globalFilter === 'preview'
          ? 'border-amber-500 text-amber-600 bg-amber-500/8'
          : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-amber-500 hover:text-amber-600'"
        :disabled="isGlobalFilterLoading"
        @click="emit('update:globalFilter', globalFilter === 'preview' ? 'none' : 'preview')"
      >
        <i class="mr-1.5" :class="isGlobalFilterLoading && globalFilter === 'preview' ? 'pi pi-spin pi-spinner' : 'pi pi-calendar'" />
        Prév. du mois
      </button>

      <div class="w-px h-7 bg-[var(--border-color)] mx-1 shrink-0" />
      <BookletActionButtons
        class="ml-auto"
        orientation="horizontal"
        :has-selection="hasSelection"
        :selected-count="selectedCount"
        :selected-amount="selectedAmount"
        :selected-amount-label="selectedAmountLabel"
        :has-regenerable-transactions="hasRegenerableTransactions"
        :is-any-action-loading="isAnyActionLoading"
        :is-delete-loading="isDeleteLoading"
        :is-export-csv-loading="isExportCsvLoading"
        :is-regenerate-loading="isRegenerateLoading"
        @new-transaction="emit('newTransaction')"
        @new-preview="emit('newPreview')"
        @import-csv="emit('importCsv')"
        @export-csv="emit('exportCsv')"
        @regenerate="emit('regenerate')"
        @delete="emit('delete')"
      />
    </template>
  </div>

  <!-- Mobile selection bar -->
  <div v-if="isMobile" class="flex flex-col gap-2 mb-2">
    <Transition name="fade">
      <div v-if="hasSelection" class="flex items-center gap-2">
        <div class="flex items-center justify-between gap-2 px-3 py-1.5 rounded-xl border border-[var(--card-border)] bg-[var(--card-bg)] shadow-sm flex-1">
          <div class="flex items-center gap-1.5">
            <i class="pi pi-check-square text-[var(--primary)] text-xs" />
            <span class="text-xs font-semibold text-[var(--text-secondary)]">{{ selectedCount }} sélectionnée{{ selectedCount > 1 ? 's' : '' }}</span>
          </div>
          <div class="w-px h-4 bg-[var(--border-color)]" />
          <span class="text-sm font-extrabold" :class="selectedAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">
            {{ selectedAmountLabel }}
          </span>
        </div>
        <Button
          v-tooltip.bottom="`Supprimer (${selectedCount})`"
          class="!w-8 !h-8 !p-0 !flex !items-center !justify-center shrink-0 !bg-red-500 !text-white !border-0 shadow-[0_2px_8px_rgba(239,68,68,0.3)] hover:!bg-red-600 transition-all"
          icon="pi pi-trash"
          :loading="isDeleteLoading"
          :disabled="isAnyActionLoading"
          @click="emit('delete')"
        />
      </div>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

:deep(.p-select) {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
}
:deep(.p-select-label) {
  background: transparent !important;
  color: var(--text-primary) !important;
  padding: 0.35rem 0.4rem !important;
}
:deep(.p-select-trigger) {
  color: var(--text-secondary) !important;
  width: 1.2rem !important;
}
:deep(.p-select.p-focus) {
  outline: none !important;
  box-shadow: none !important;
  border-color: var(--card-border) !important;
}
</style>
