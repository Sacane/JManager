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
  <div class="bg-[var(--card-bg)] rounded-2xl p-5 shadow border border-[var(--card-border)] overflow-hidden mb-5 lg:(p-4 rounded-xl) md:(p-3 rounded-lg mb-4)">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4 md:gap-4">
      <div class="flex items-center gap-4 min-w-0 md:gap-3">
        <Button
          class="text-[var(--primary)] w-9 h-9 rounded-full grid place-items-center hover:bg-[rgba(130,42,204,0.1)]"
          icon="pi pi-arrow-left"
          text
          rounded
          @click="emit('back')"
        />
        <div class="flex-1 min-w-0">
          <h1 class="text-2xl font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0 md:(text-xl mb-1)">
            {{ displayLabel }}
          </h1>
          <div class="flex gap-4 flex-wrap md:gap-2.5">
            <span class="inline-flex items-center text-sm font-semibold text-[var(--text-secondary)]">{{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}</span>
            <span v-if="previewTransactionsCount > 0" class="text-amber-600 inline-flex items-center text-sm font-semibold">{{ previewTransactionsCount }} en attente</span>
          </div>
        </div>
      </div>
      <!-- Right: balances + filters -->
      <div class="flex flex-col items-stretch gap-3 shrink-0 w-full md:(w-auto flex-row items-center gap-6)">
        <div class="flex items-center justify-between w-full md:w-auto gap-4 p-3 bg-gradient-to-br from-[var(--bg-tertiary)] to-[var(--bg-secondary)] rounded-xl border border-[var(--card-border)] md:(p-2.5 gap-4)">
          <div class="flex flex-col gap-1">
            <span class="text-[0.69rem] font-semibold uppercase tracking-wider text-[var(--text-tertiary)] md:text-2xs">Réel</span>
            <span class="text-xl font-extrabold text-[var(--primary)] md:text-lg">{{ realSold.toFixed(2) }} €</span>
          </div>
          <div class="w-px h-10 md:h-9 bg-gradient-to-b from-transparent via-[var(--border-color)] to-transparent" />
          <div class="flex flex-col gap-1">
            <span class="text-[0.69rem] font-semibold uppercase tracking-wider text-[var(--text-tertiary)] md:text-2xs">Prévisionnel</span>
            <span class="text-xl font-extrabold text-[#FF5A08] md:text-lg">{{ previewSold.toFixed(2) }} €</span>
          </div>
        </div>

        <div class="flex w-full md:w-auto gap-2 items-center">
          <Select
            :model-value="selectedMonth"
            :options="monthOptions"
            placeholder="Mois"
            class="flex-1 min-w-0 w-full md:(flex-none min-w-[120px] w-auto) border-1 rounded-lg bg-transparent"
            @update:model-value="(val: string) => emit('update:selectedMonth', val)"
            @change="emit('month-change')"
          />
          <DatePicker
            :model-value="dateYear"
            view="year"
            date-format="yy"
            class="flex-1 min-w-0 w-full md:(flex-none min-w-[220px] w-[220px]) rounded-[14px] min-h-[46px] cursor-pointer bg-transparent"
            placeholder="Année"
            :show-icon="true"
            icon-display="input"
            @update:model-value="(val: Date) => emit('update:dateYear', val)"
            @date-select="emit('year-change')"
          />

        </div>
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
