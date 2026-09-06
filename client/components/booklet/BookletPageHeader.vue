<script setup lang="ts">
interface Props {
  displayLabel: string
  transactionsCount: number
  previewTransactionsCount: number
  realSold: number
  previewSold: number
  isMobile: boolean
  /** Active period, shown on the period trigger. */
  periodLabel: string
  monthLabel: string
  hasCustomRange: boolean
  rangeStart: Date | null
  rangeEnd: Date | null
  dateRangeError: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  'back': []
  'update:rangeStart': [val: Date | null]
  'update:rangeEnd': [val: Date | null]
  'previousMonth': []
  'nextMonth': []
  'currentMonth': []
  'rollingWindow': [days: number]
  'applyRange': []
  'clearRange': []
}>()
</script>

<template>
  <div class="bg-[var(--card-bg)] rounded-xl shadow border border-[var(--card-border)] overflow-hidden mb-2">
    <!-- Mobile layout: single compact row -->
    <template v-if="isMobile">
      <div class="flex items-start gap-2 px-3 py-2.5">
        <Button
          class="text-[var(--accent-orange)] !w-8 !h-8 rounded-full grid place-items-center hover:bg-[var(--accent-orange)]/10 shrink-0 mt-0.5"
          icon="pi pi-arrow-left"
          text
          rounded
          aria-label="Retour aux livrets"
          @click="emit('back')"
        />

        <div class="flex-1 min-w-0">
          <h1 class="text-lg font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0 leading-tight truncate">
            {{ displayLabel }}
          </h1>
          <div class="flex items-center gap-3 mt-1.5">
            <div class="flex flex-col gap-0">
              <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Réel</span>
              <span class="text-base font-extrabold tabular-nums text-[var(--primary)] whitespace-nowrap leading-tight">{{ realSold.toFixed(2) }}&nbsp;€</span>
            </div>
            <span class="text-[var(--text-muted)] text-xs self-end mb-0.5">·</span>
            <div class="flex flex-col gap-0">
              <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Prévis.</span>
              <span class="text-base font-extrabold tabular-nums text-[var(--accent-orange)] whitespace-nowrap leading-tight">{{ previewSold.toFixed(2) }}&nbsp;€</span>
            </div>
          </div>
        </div>

        <BookletPeriodPicker
          compact
          class="shrink-0 mt-0.5"
          :period-label="periodLabel"
          :month-label="monthLabel"
          :has-custom-range="hasCustomRange"
          :range-start="rangeStart"
          :range-end="rangeEnd"
          :date-range-error="dateRangeError"
          @update:range-start="emit('update:rangeStart', $event)"
          @update:range-end="emit('update:rangeEnd', $event)"
          @previous-month="emit('previousMonth')"
          @next-month="emit('nextMonth')"
          @current-month="emit('currentMonth')"
          @rolling-window="emit('rollingWindow', $event)"
          @apply-range="emit('applyRange')"
          @clear-range="emit('clearRange')"
        />
      </div>
    </template>

    <!-- Desktop layout -->
    <template v-else>
      <div class="flex items-center justify-between gap-2 flex-wrap py-1 px-3">
        <div class="flex items-center gap-2 min-w-0">
          <Button
            class="text-[var(--accent-orange)] !w-7 !h-7 rounded-full grid place-items-center hover:bg-[var(--accent-orange)]/10 shrink-0"
            icon="pi pi-arrow-left"
            text
            rounded
            aria-label="Retour aux livrets"
            @click="emit('back')"
          />
          <div class="flex items-baseline gap-2 min-w-0 flex-wrap">
            <h1 class="text-base font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0 leading-tight truncate">
              {{ displayLabel }}
            </h1>
            <span class="text-xs font-semibold text-[var(--text-secondary)] whitespace-nowrap">{{ transactionsCount }} trans.</span>
            <span v-if="previewTransactionsCount > 0" class="text-[var(--accent-orange)] text-xs font-semibold whitespace-nowrap">{{ previewTransactionsCount }} en attente</span>
          </div>
        </div>

        <div class="flex items-center gap-2 shrink-0 flex-wrap">
          <div class="flex items-center gap-2 sm:gap-4 px-3 py-1.5 sm:(px-5 py-2.5) bg-gradient-to-br from-[var(--bg-tertiary)] to-[var(--bg-secondary)] rounded-xl border border-[var(--card-border)] shadow-sm">
            <div class="flex flex-col items-center gap-0.5">
              <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Réel</span>
              <span class="text-sm sm:text-xl font-extrabold tabular-nums text-[var(--primary)]">{{ realSold.toFixed(2) }} €</span>
            </div>
            <div class="w-px h-6 sm:h-10 bg-[var(--border-color)]" />
            <div class="flex flex-col items-center gap-0.5">
              <span class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)]">Prévis.</span>
              <span class="text-sm sm:text-xl font-extrabold tabular-nums text-[var(--accent-orange)]">{{ previewSold.toFixed(2) }} €</span>
            </div>
          </div>

          <BookletPeriodPicker
            :period-label="periodLabel"
            :month-label="monthLabel"
            :has-custom-range="hasCustomRange"
            :range-start="rangeStart"
            :range-end="rangeEnd"
            :date-range-error="dateRangeError"
            @update:range-start="emit('update:rangeStart', $event)"
            @update:range-end="emit('update:rangeEnd', $event)"
            @previous-month="emit('previousMonth')"
            @next-month="emit('nextMonth')"
            @current-month="emit('currentMonth')"
            @rolling-window="emit('rollingWindow', $event)"
            @apply-range="emit('applyRange')"
            @clear-range="emit('clearRange')"
          />
        </div>
      </div>
    </template>
  </div>
</template>
