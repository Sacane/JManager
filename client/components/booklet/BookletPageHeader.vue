<script setup lang="ts">
interface Props {
  displayLabel: string
  transactionsCount: number
  previewTransactionsCount: number
  realSold: number
  previewSold: number
  isMobile: boolean
  selectedMonth: string
  monthOptions: string[]
  dateYear: Date
}

defineProps<Props>()

const emit = defineEmits<{
  'update:selectedMonth': [val: string]
  'month-change': []
  'update:dateYear': [val: Date]
  'year-change': []
  'back': []
}>()
</script>

<template>
  <div class="bg-[var(--card-bg)] rounded-xl py-1 px-3 shadow border border-[var(--card-border)] overflow-hidden mb-2">
    <div class="flex items-center justify-between gap-2 flex-wrap">
      <!-- Left: back + title + counts -->
      <div class="flex items-center gap-2 min-w-0">
        <Button
          class="text-[#FF5A08] !w-7 !h-7 rounded-full grid place-items-center hover:bg-[#FF5A08]/10 shrink-0"
          icon="pi pi-arrow-left"
          text
          rounded
          @click="emit('back')"
        />
        <div class="flex items-baseline gap-2 min-w-0 flex-wrap">
          <h1 class="text-base font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0 leading-tight truncate">
            {{ displayLabel }}
          </h1>
          <span class="text-xs font-semibold text-[var(--text-secondary)] whitespace-nowrap">{{ transactionsCount }} trans.</span>
          <span v-if="previewTransactionsCount > 0" class="text-[#FF5A08] text-xs font-semibold whitespace-nowrap">{{ previewTransactionsCount }} en attente</span>
        </div>
      </div>

      <!-- Right: balances + filters -->
      <div class="flex items-center gap-2 shrink-0 flex-wrap">
        <!-- Balances -->
        <div class="flex items-center gap-2 sm:gap-4 px-3 py-1.5 sm:(px-5 py-2.5) bg-gradient-to-br from-[var(--bg-tertiary)] to-[var(--bg-secondary)] rounded-xl border border-[var(--card-border)] shadow-sm">
          <div class="flex flex-col items-center gap-0.5">
            <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Réel</span>
            <span class="text-sm sm:text-xl font-extrabold tabular-nums text-[var(--primary)]">{{ realSold.toFixed(2) }} €</span>
          </div>
          <div class="w-px h-6 sm:h-10 bg-[var(--border-color)]" />
          <div class="flex flex-col items-center gap-0.5">
            <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Prévis.</span>
            <span class="text-sm sm:text-xl font-extrabold tabular-nums text-[#FF5A08]">{{ previewSold.toFixed(2) }} €</span>
          </div>
        </div>

        <!-- Month + Year -->
        <Select
          :model-value="selectedMonth"
          :options="monthOptions"
          placeholder="Mois"
          class="!w-[90px] sm:!w-[120px] border-1 rounded-lg bg-transparent"
          size="small"
          @update:model-value="(val: string) => emit('update:selectedMonth', val)"
          @change="emit('month-change')"
        />
        <DatePicker
          :model-value="dateYear"
          view="year"
          date-format="yy"
          class="!w-[70px] sm:!w-[90px] rounded-lg cursor-pointer bg-transparent"
          placeholder="Année"
          :show-icon="false"
          @update:model-value="(val: Date) => emit('update:dateYear', val)"
          @date-select="emit('year-change')"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
:deep(.p-dropdown),
:deep(.p-select),
:deep(.p-calendar) {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
}
:deep(.p-inputtext),
:deep(.p-dropdown-label),
:deep(.p-select-label) {
  background: transparent !important;
  color: var(--text-primary) !important;
}
:deep(.p-inputtext) {
  border: 1px solid var(--card-border) !important;
}
:deep(.p-dropdown-trigger),
:deep(.p-datepicker-trigger),
:deep(.p-select-trigger),
:deep(.p-icon) {
  color: var(--text-secondary) !important;
}
:deep(.p-inputtext:focus),
:deep(.p-inputtext:focus-visible),
:deep(.p-inputwrapper-focus .p-inputtext),
:deep(.p-dropdown.p-focus),
:deep(.p-calendar.p-focus),
:deep(.p-calendar:focus-within),
:deep(.p-focus) {
  outline: none !important;
  box-shadow: none !important;
  border-color: var(--card-border) !important;
}
</style>
