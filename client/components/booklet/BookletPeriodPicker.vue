<script setup lang="ts">
/**
 * Single entry point for the period shown on a booklet.
 *
 * It replaces the month select, the year picker and the inline range bar that used to sit in the
 * header — six controls for one notion, which crowded the header and let the month and the range
 * navigate against each other. Everything now goes through one button and one panel.
 */
interface Props {
  /** Human-readable active period, shown on the button. */
  periodLabel: string
  monthLabel: string
  hasCustomRange: boolean
  rangeStart: Date | null
  rangeEnd: Date | null
  dateRangeError: string | null
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), { compact: false })

const emit = defineEmits<{
  'update:rangeStart': [value: Date | null]
  'update:rangeEnd': [value: Date | null]
  'previousMonth': []
  'nextMonth': []
  'currentMonth': []
  'rollingWindow': [days: number]
  'applyRange': []
  'clearRange': []
}>()

const panel = ref()

function toggle(event: Event): void {
  panel.value?.toggle(event)
}

const ROLLING_WINDOWS = [
  { days: 30, label: '30 derniers jours' },
  { days: 90, label: '90 derniers jours' },
]
</script>

<template>
  <div>
    <button
      type="button"
      class="period-trigger"
      :class="{ 'period-trigger--compact': props.compact, 'period-trigger--custom': props.hasCustomRange }"
      data-test="open-period-picker"
      :aria-label="`Période affichée : ${props.periodLabel}. Modifier`"
      @click="toggle"
    >
      <i class="pi pi-calendar" aria-hidden="true" />
      <span class="period-trigger__label">{{ props.periodLabel }}</span>
      <i class="pi pi-chevron-down text-[0.65rem]" aria-hidden="true" />
    </button>

    <Popover ref="panel">
      <div class="period-panel" data-test="period-panel">
        <div class="period-panel__section">
          <span class="period-panel__title">Mois</span>
          <div class="period-panel__month-nav">
            <Button
              icon="pi pi-chevron-left"
              text
              rounded
              aria-label="Mois précédent"
              data-test="previous-month"
              @click="emit('previousMonth')"
            />
            <span class="period-panel__month">{{ props.monthLabel }}</span>
            <Button
              icon="pi pi-chevron-right"
              text
              rounded
              aria-label="Mois suivant"
              data-test="next-month"
              @click="emit('nextMonth')"
            />
          </div>
          <Button
            label="Mois en cours"
            size="small"
            text
            data-test="current-month"
            @click="emit('currentMonth')"
          />
        </div>

        <div class="period-panel__section">
          <span class="period-panel__title">Raccourcis</span>
          <div class="period-panel__shortcuts">
            <Button
              v-for="window in ROLLING_WINDOWS"
              :key="window.days"
              :label="window.label"
              size="small"
              outlined
              :data-test="`rolling-${window.days}`"
              @click="emit('rollingWindow', window.days)"
            />
          </div>
        </div>

        <div class="period-panel__section">
          <span class="period-panel__title">Période personnalisée</span>
          <div class="period-panel__range">
            <DatePicker
              :model-value="props.rangeStart"
              date-format="dd/mm/yy"
              placeholder="Début"
              show-icon
              icon-display="input"
              data-test="range-start"
              @update:model-value="(value: Date) => emit('update:rangeStart', value)"
            />
            <DatePicker
              :model-value="props.rangeEnd"
              date-format="dd/mm/yy"
              placeholder="Fin"
              show-icon
              icon-display="input"
              data-test="range-end"
              @update:model-value="(value: Date) => emit('update:rangeEnd', value)"
            />
          </div>

          <p v-if="props.dateRangeError" class="period-panel__error" role="alert" data-test="range-error">
            {{ props.dateRangeError }}
          </p>

          <div class="period-panel__actions">
            <Button
              v-if="props.hasCustomRange"
              label="Revenir au mois"
              size="small"
              text
              severity="secondary"
              data-test="clear-range"
              @click="emit('clearRange')"
            />
            <Button
              label="Appliquer"
              size="small"
              data-test="apply-range"
              @click="emit('applyRange')"
            />
          </div>
        </div>
      </div>
    </Popover>
  </div>
</template>

<style scoped>
.period-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.7rem;
  border: 1px solid var(--card-border);
  border-radius: 0.6rem;
  background: var(--card-bg);
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease;
  max-width: 100%;
}

.period-trigger:hover {
  border-color: var(--primary);
  color: var(--primary);
}

/* A custom range is a deliberate, temporary state: the trigger says so at a glance. */
.period-trigger--custom {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(var(--primary-rgb), 0.06);
}

.period-trigger--compact {
  padding: 0.3rem 0.5rem;
  font-size: 0.72rem;
}

.period-trigger__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.period-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 17rem;
  max-width: min(22rem, 90vw);
}

.period-panel__section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.period-panel__title {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.period-panel__month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.period-panel__month {
  font-weight: 700;
  color: var(--text-primary);
}

.period-panel__shortcuts {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.period-panel__range {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.period-panel__error {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--danger);
}

.period-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
