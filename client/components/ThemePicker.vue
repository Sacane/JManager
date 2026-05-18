<script setup lang="ts">
import type { ThemePreference } from '~/composables/useDark'

interface ThemeOption {
  value: ThemePreference
  label: string
  icon: string
}

const { preference, setPreference } = useDark()

const options: ThemeOption[] = [
  { value: 'light', label: 'Clair', icon: 'pi pi-sun' },
  { value: 'dark', label: 'Sombre', icon: 'pi pi-moon' },
  { value: 'system', label: 'Système', icon: 'pi pi-desktop' },
]
</script>

<template>
  <div class="theme-picker" role="radiogroup" aria-label="Thème de l'application">
    <button
      v-for="option in options"
      :key="option.value"
      class="theme-card"
      :class="{ 'theme-card--active': preference === option.value }"
      role="radio"
      :aria-checked="String(preference === option.value)"
      :data-test="`theme-option-${option.value}`"
      @click="setPreference(option.value)"
    >
      <i :class="option.icon" class="theme-card__icon" />
      <span class="theme-card__label">{{ option.label }}</span>
    </button>
  </div>
</template>

<style scoped lang="scss">
.theme-picker {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.theme-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem 1.75rem;
  min-width: 88px;
  border-radius: 0.875rem;
  border: 1.5px solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;

  &:hover {
    border-color: var(--primary-lighter);
    color: var(--primary);
    background: rgba(101, 8, 204, 0.04);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(101, 8, 204, 0.12);
  }

  &:active {
    transform: translateY(0);
  }

  &--active {
    border: 2px solid var(--primary);
    color: var(--primary);
    background: rgba(101, 8, 204, 0.06);
    box-shadow: 0 2px 8px rgba(101, 8, 204, 0.15);

    html.dark & {
      background: rgba(101, 8, 204, 0.15);
      box-shadow: 0 2px 12px rgba(101, 8, 204, 0.3);
    }
  }
}

.theme-card__icon {
  font-size: 1.35rem;
}

.theme-card__label {
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}
</style>
