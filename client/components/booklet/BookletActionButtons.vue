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
  newTransaction: []
  newPreview: []
  importCsv: []
  exportCsv: []
  regenerate: []
  delete: []
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
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-[#A30053] !from-[var(--primary)] !to-[var(--primary-2)] !text-white !border-0 shadow-[0_2px_8px_rgba(101,8,204,0.3)] hover:!brightness-110 transition-all"
      icon="pi pi-plus"
      :disabled="isAnyActionLoading"
      @click="emit('newTransaction')"
    />
    <Button
      v-tooltip.bottom="'Transaction prévisionnelle'"
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-[#FFC108] !text-white !border-0 shadow-[0_2px_8px_rgba(255,193,8,0.3)] hover:!bg-[#d9a307] transition-all"
      icon="pi pi-clock"
      :disabled="isAnyActionLoading"
      @click="emit('newPreview')"
    />
    <Button
      v-if="hasRegenerableTransactions"
      v-tooltip.bottom="'Régénérer les transactions supprimées'"
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-[#A30053] !text-white !border-0 shadow-[0_2px_8px_rgba(255,193,8,0.3)] hover:!bg-[#d9a307] transition-all"
      icon="pi pi-refresh"
      :loading="isRegenerateLoading"
      :disabled="isAnyActionLoading"
      @click="emit('regenerate')"
    />
    <Button
      v-tooltip.bottom="'Importer CSV'"
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-[#009CFE] !text-white !border-0 shadow-[0_2px_8px_rgba(0,156,254,0.3)] hover:!bg-[#006EA3] transition-all"
      icon="pi pi-file-import"
      :disabled="isAnyActionLoading"
      @click="emit('importCsv')"
    />
    <Button
      v-tooltip.bottom="'Exporter CSV'"
      aria-label="Exporter CSV"
      class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-[#009CFE] !text-white !border-0 shadow-[0_2px_8px_rgba(0,156,254,0.3)] hover:!bg-[#006EA3] transition-all"
      icon="pi pi-file-export"
      :loading="isExportCsvLoading"
      :disabled="isAnyActionLoading"
      @click="emit('exportCsv')"
    />

    <template v-if="hasSelection">
      <!-- Selection chip (vertical mode) — count + amount -->
      <div
        v-if="orientation === 'vertical'"
        class="flex flex-col items-center gap-0.5 px-2 py-1.5 rounded-lg bg-[var(--card-hover-bg)] border border-[var(--card-border)] w-full mt-1"
      >
        <div class="flex items-center gap-1">
          <i class="pi pi-check-square text-[var(--primary)] text-xs" />
          <span class="text-xs font-semibold text-[var(--text-secondary)]">{{ selectedCount }}</span>
        </div>
        <span class="text-xs font-extrabold" :class="selectedAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">{{ selectedAmountLabel }}</span>
      </div>
      <Button
        v-tooltip.bottom="`Supprimer (${selectedCount})`"
        :outlined="orientation === 'vertical'"
        :class="orientation === 'vertical'
          ? '!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-red-500 !text-white !border-0 shadow-[0_2px_8px_rgba(239,68,68,0.3)] hover:!bg-red-600 transition-all'
          : '!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 !bg-red-500 !text-white !border-0 shadow-[0_2px_8px_rgba(239,68,68,0.3)] hover:!bg-red-600 transition-all'"
        icon="pi pi-trash"
        :loading="isDeleteLoading"
        :disabled="isAnyActionLoading"
        @click="emit('delete')"
      />
    </template>
  </div>
</template>
