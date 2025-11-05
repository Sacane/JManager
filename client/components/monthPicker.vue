<script setup lang="ts">
const emits = defineEmits(['update:modelValue'])

const date = useDate()

const selectedMonth = ref('')
const currentMonth = ref(date.translate(date.monthFromNumber(new Date().getMonth() + 1) as string))

watchEffect(() => {
  emits('update:modelValue', selectedMonth.value)
})

function onMonthChange() {
  emits('update:modelValue', selectedMonth.value)
}
</script>

<template>
  <div class="month-picker-wrapper">
    <label for="monthSelect" class="month-picker-label">Choisissez un mois :</label>
    <div class="month-picker-container">
      <select
        id="monthSelect"
        v-model="selectedMonth"
        class="month-picker-select"
        @change="onMonthChange"
      >
        <option :value="currentMonth" disabled hidden>
          Choisissez un mois
        </option>
        <option v-for="(month, index) in date.months" :key="index" :value="month">
          {{ month }}
        </option>
      </select>
      <i class="month-picker-icon pi pi-chevron-down" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.month-picker-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.month-picker-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.01em;
}

.month-picker-container {
  position: relative;
  width: 100%;
}

.month-picker-select {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 1rem;
  border-radius: 10px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 0.9375rem;
  font-weight: 600;
  outline: none;
  transition: all 0.3s ease;
  appearance: none;
  cursor: pointer;

  &:hover {
    border-color: var(--primary);
    background: var(--card-hover-bg);
  }

  &:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 2px rgba(130, 42, 204, 0.1);
  }

  option {
    background: var(--card-bg);
    color: var(--text-primary);
    padding: 0.75rem 1rem;
    font-weight: 500;

    &:hover {
      background: var(--card-hover-bg);
    }
  }
}

.month-picker-icon {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
  font-size: 0.875rem;
  pointer-events: none;
  transition: color 0.3s ease;
}

.month-picker-select:hover + .month-picker-icon {
  color: var(--primary);
}
</style>
