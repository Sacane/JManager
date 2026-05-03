<script setup lang="ts">
interface Props {
  orientation?: 'horizontal' | 'vertical'
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

withDefaults(defineProps<Props>(), {
  orientation: 'horizontal',
})

const emit = defineEmits<{
  'new-transaction': []
  'new-preview': []
  'import-csv': []
  'export-csv': []
  'regenerate': []
  'delete': []
}>()
</script>

<template>
  <div :class="orientation === 'vertical' ? 'flex flex-col items-center gap-1.5' : 'flex items-center gap-1.5 shrink-0'">
    <!-- Selection info badge (horizontal mode) -->
    <template v-if="hasSelection && orientation === 'horizontal'">
      <span class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-[var(--card-bg)] border border-[var(--card-border)] text-sm font-semibold whitespace-nowrap shrink-0">
        <i class="pi pi-check-square text-[var(--primary)] text-xs" />
        <span class="text-[var(--text-secondary)]">{{ selectedCount }}</span>
        <span class="w-px h-4 bg-[var(--border-color)] inline-block" />
        <span :class="selectedAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">{{ selectedAmountLabel }}</span>
      </span>
      <div class="w-px h-7 bg-[var(--border-color)] mx-1 shrink-0" />
    </template>

    <Button
      v-tooltip.bottom="'Nouvelle transaction'"
      class="btn-primary !w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0"
      icon="pi pi-plus"
      :disabled="isAnyActionLoading"
      @click="emit('new-transaction')"
    />
    <Button
      v-tooltip.bottom="'Transaction prévisionnelle'"
      outlined
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-amber-500 text-amber-600 hover:bg-amber-500/10 transition-all"
      icon="pi pi-clock"
      :disabled="isAnyActionLoading"
      @click="emit('new-preview')"
    />
    <Button
      v-tooltip.bottom="'Importer CSV'"
      outlined
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-cyan-500 text-cyan-600 hover:bg-cyan-500/10 transition-all"
      icon="pi pi-file-import"
      :disabled="isAnyActionLoading"
      @click="emit('import-csv')"
    />
    <Button
      v-tooltip.bottom="'Exporter CSV'"
      aria-label="Exporter CSV"
      outlined
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-emerald-500 text-emerald-600 hover:bg-emerald-500/10 transition-all"
      icon="pi pi-file-export"
      :loading="isExportCsvLoading"
      :disabled="isAnyActionLoading"
      @click="emit('export-csv')"
    />
    <Button
      v-if="hasRegenerableTransactions"
      v-tooltip.bottom="'Régénérer les transactions supprimées'"
      outlined
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-violet-500 text-violet-600 hover:bg-violet-500/10 transition-all"
      icon="pi pi-refresh"
      :loading="isRegenerateLoading"
      :disabled="isAnyActionLoading"
      @click="emit('regenerate')"
    />

    <template v-if="hasSelection">
      <!-- Selection count chip (vertical mode) -->
      <div
        v-if="orientation === 'vertical'"
        class="flex items-center justify-center gap-1 px-2 py-1 rounded-lg bg-[var(--card-bg)] border border-[var(--card-border)] w-full mt-1"
      >
        <i class="pi pi-check-square text-[var(--primary)] text-xs" />
        <span class="text-xs font-semibold text-[var(--text-secondary)]">{{ selectedCount }}</span>
      </div>
      <Button
        v-tooltip.bottom="`Supprimer (${selectedCount})`"
        outlined
        class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-red-500 text-red-500 hover:bg-red-500/10 transition-all"
        icon="pi pi-trash"
        :loading="isDeleteLoading"
        :disabled="isAnyActionLoading"
        @click="emit('delete')"
      />
    </template>
  </div>
</template>
