<script setup lang="ts">
interface Props {
  isMobile: boolean
  hideActionButtons?: boolean
  transactionFilter: 'all' | 'preview' | 'confirmed'
  transactionsCount: number
  previewTransactionsCount: number
  hasSelection: boolean
  selectedCount: number
  selectedAmount: number
  selectedAmountLabel: string
  hasRegenerableTransactions: boolean
  isAnyActionLoading: boolean
  isDeleteLoading: boolean
  isExportCsvLoading: boolean
  isRegenerateLoading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  hideActionButtons: false,
})

const emit = defineEmits<{
  'update:transactionFilter': [val: 'all' | 'preview' | 'confirmed']
  'new-transaction': []
  'new-preview': []
  'import-csv': []
  'export-csv': []
  'regenerate': []
  'delete': []
}>()
</script>

<template>
  <!-- Filtres + Actions -->
  <div v-if="isMobile || !hideActionButtons" class="flex items-center gap-2 mb-4 overflow-x-auto pb-2">
    <template v-if="isMobile">
      <span class="text-sm font-semibold text-[var(--text-secondary)] whitespace-nowrap mr-1 shrink-0">Afficher :</span>
      <button
        class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
        :class="transactionFilter === 'all'
          ? 'bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-[0_2px_8px_rgba(130,42,204,0.25)]'
          : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-[var(--primary)]'"
        @click="emit('update:transactionFilter', 'all')"
      >
        <i class="pi pi-list mr-2" />
        Tout ({{ transactionsCount }})
      </button>
      <button
        class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
        :class="transactionFilter === 'confirmed'
          ? 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white shadow-[0_2px_8px_rgba(16,185,129,0.25)]'
          : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-emerald-600'"
        @click="emit('update:transactionFilter', 'confirmed')"
      >
        <i class="pi pi-check-circle mr-2" />
        Confirmées ({{ transactionsCount - previewTransactionsCount }})
      </button>
      <button
        class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
        :class="transactionFilter === 'preview'
          ? 'bg-gradient-to-br from-amber-500 to-amber-600 text-white shadow-[0_2px_8px_rgba(245,158,11,0.25)]'
          : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-amber-600'"
        @click="emit('update:transactionFilter', 'preview')"
      >
        <i class="pi pi-clock mr-2" />
        Prévisionnelles ({{ previewTransactionsCount }})
      </button>
    </template>

    <!-- Filtres + Actions : desktop uniquement -->
    <template v-if="!isMobile">
      <button
        class="px-3 py-1.5 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0 border"
        :class="transactionFilter === 'all'
          ? 'border-[var(--primary)] text-[var(--primary)] bg-[rgba(130,42,204,0.07)]'
          : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-[var(--primary)] hover:text-[var(--primary)]'"
        @click="emit('update:transactionFilter', 'all')"
      >
        <i class="pi pi-list mr-1.5" />
        Tout ({{ transactionsCount }})
      </button>
      <button
        class="px-3 py-1.5 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0 border"
        :class="transactionFilter === 'confirmed'
          ? 'border-emerald-500 text-emerald-600 bg-emerald-500/8'
          : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-emerald-500 hover:text-emerald-600'"
        @click="emit('update:transactionFilter', 'confirmed')"
      >
        <i class="pi pi-check-circle mr-1.5" />
        Confirmées ({{ transactionsCount - previewTransactionsCount }})
      </button>
      <button
        class="px-3 py-1.5 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0 border"
        :class="transactionFilter === 'preview'
          ? 'border-amber-500 text-amber-600 bg-amber-500/8'
          : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-amber-500 hover:text-amber-600'"
        @click="emit('update:transactionFilter', 'preview')"
      >
        <i class="pi pi-clock mr-1.5" />
        Prévisionnelles ({{ previewTransactionsCount }})
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
        @new-transaction="emit('new-transaction')"
        @new-preview="emit('new-preview')"
        @import-csv="emit('import-csv')"
        @export-csv="emit('export-csv')"
        @regenerate="emit('regenerate')"
        @delete="emit('delete')"
      />
    </template>
  </div>

  <!-- Boutons d'action mobile -->
  <div v-if="isMobile" class="flex gap-2 mb-4">
    <Button
      class="flex-1 !bg-gradient-to-br !from-[var(--primary)] !to-[var(--primary-2)] !text-white !border-0 font-semibold shadow-[0_2px_8px_rgba(130,42,204,0.3)] hover:!brightness-110 transition-all"
      icon="pi pi-plus"
      label="Transaction"
      :disabled="isAnyActionLoading"
      @click="emit('new-transaction')"
    />
    <Button
      class="flex-1 !bg-amber-500 !text-white !border-0 font-semibold shadow-[0_2px_8px_rgba(245,158,11,0.3)] hover:!bg-amber-600 transition-all"
      icon="pi pi-clock"
      label="Prévisionnelle"
      :disabled="isAnyActionLoading"
      @click="emit('new-preview')"
    />
    <Button
      v-if="hasRegenerableTransactions"
      class="flex-1 !bg-violet-500 !text-white !border-0 font-semibold shadow-[0_2px_8px_rgba(139,92,246,0.3)] hover:!bg-violet-600 transition-all"
      icon="pi pi-refresh"
      label="Régénérer"
      :loading="isRegenerateLoading"
      :disabled="isAnyActionLoading"
      @click="emit('regenerate')"
    />
  </div>

  <!-- Mobile selection bar -->
  <div v-if="isMobile" class="flex flex-col gap-3 mb-5 md:mb-4">
    <Transition name="fade">
      <div v-if="hasSelection" class="flex items-center gap-3">
        <div class="flex items-center justify-between gap-4 px-4 py-2.5 rounded-xl border border-[var(--card-border)] bg-[var(--card-bg)] shadow-sm flex-1">
          <div class="flex items-center gap-2">
            <i class="pi pi-check-square text-[var(--primary)] text-sm" />
            <span class="text-sm font-semibold text-[var(--text-secondary)]">{{ selectedCount }} sélectionnée{{ selectedCount > 1 ? 's' : '' }}</span>
          </div>
          <div class="w-px h-6 bg-[var(--border-color)]" />
          <span class="text-base font-extrabold" :class="selectedAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">
            {{ selectedAmountLabel }}
          </span>
        </div>
        <Button
          v-tooltip.bottom="`Supprimer (${selectedCount})`"
          class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-red-500 !text-white !border-0 shadow-[0_2px_8px_rgba(239,68,68,0.3)] hover:!bg-red-600 transition-all"
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

:deep(.p-button.btn-primary) {
  background: transparent !important;
  border: 2px solid var(--primary) !important;
  color: var(--primary) !important;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(130, 42, 204, 0.15);
}
:deep(.p-button.btn-primary:hover) {
  background: rgba(130, 42, 204, 0.1) !important;
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.btn-primary:active) {
  transform: translateY(0);
}
:deep(.p-button.p-button-outlined) {
  font-weight: 600;
  transition: all 0.2s ease;
}
:deep(.p-button.p-button-outlined.border-amber-500) {
  border-color: rgb(245 158 11) !important;
  color: rgb(217 119 6) !important;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
}
:deep(.p-button.p-button-outlined.border-amber-500:hover) {
  background: rgba(245, 158, 11, 0.1) !important;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.p-button-outlined.border-cyan-500) {
  border-color: rgb(6 182 212) !important;
  color: rgb(8 145 178) !important;
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.15);
}
:deep(.p-button.p-button-outlined.border-cyan-500:hover) {
  background: rgba(6, 182, 212, 0.1) !important;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.p-button-outlined.border-emerald-500) {
  border-color: rgb(16 185 129) !important;
  color: rgb(5 150 105) !important;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
}
:deep(.p-button.p-button-outlined.border-emerald-500:hover) {
  background: rgba(16, 185, 129, 0.1) !important;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25) !important;
  transform: translateY(-1px);
}
</style>
