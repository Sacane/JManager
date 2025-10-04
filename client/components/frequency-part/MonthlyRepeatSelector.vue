<script setup lang="ts">
import InputNumber from 'primevue/inputnumber'
import { computed, ref, watch } from 'vue'

export interface MonthlyRepeatPropertyDTO {
  repeatDay: number
}

const props = defineProps<{
  repeatDay: number | null
}>()

const emit = defineEmits<{
  (e: 'update:repeatDay', value: number | null): void
}>()

const isEnabled = ref(props.repeatDay !== null)
const selectedDay = ref(props?.repeatDay ?? 1)

const errorMessage = ref<string | null>(null)

const isDayValid = computed(() => {
  return selectedDay.value >= 1 && selectedDay.value <= 31
})

// Mise à jour du jour sélectionné
function updateDay(value: number | null) {
  if (value === null || value === undefined) {
    selectedDay.value = 1
    return
  }

  selectedDay.value = value
  errorMessage.value = null

  if (value < 1 || value > 31) {
    errorMessage.value = 'Le jour doit être entre 1 et 31'
    return
  }

  if (isEnabled.value) {
    emit('update:repeatDay', value)
  }
}

// Activer/désactiver la sélection du jour
function toggleEnabled(enabled: boolean) {
  isEnabled.value = enabled

  if (enabled) {
    emit('update:repeatDay', selectedDay.value)
  } else {
    emit('update:repeatDay', null)
  }
}

// Synchroniser avec les changements externes
watch(() => selectedDay, (newValue) => {
  if (newValue === null) {
    isEnabled.value = false
  } else {
    isEnabled.value = true
    selectedDay.value = newValue.value
    emit('update:repeatDay', newValue.value)
  }
})

// Texte d'aide pour expliquer le comportement
const helpText = computed(() => {
  if (!isEnabled.value) {
    return 'La transaction sera répétée le même jour que la date de début chaque mois'
  }

  if (selectedDay.value > 28) {
    return `Pour les mois courts, la transaction sera effectuée le dernier jour du mois`
  }

  return `La transaction sera répétée chaque ${selectedDay.value}${getDaySuffix(selectedDay.value)} du mois`
})

function getDaySuffix(day: number): string {
  if (day === 1) return 'er'
  return 'ème'
}
</script>

<template>
  <div class="monthly-repeat-selector">
    <div class="form-group">
      <div class="toggle-section">
        <div class="flex items-center gap-2">
          <ToggleSwitch
            id="enableDaySelection"
            :model-value="isEnabled"
            @update:model-value="toggleEnabled"
          />
          <label for="enableDaySelection" class="font-medium">
            Définir un jour spécifique du mois
          </label>
        </div>
      </div>

      <Transition name="fade">
        <div v-if="isEnabled" class="day-input-section">
          <label for="repeatDay" class="block mb-2 font-medium">
            Jour de répétition
          </label>
          <InputNumber
            id="repeatDay"
            :model-value="selectedDay"
            :min="1"
            :max="31"
            :step="1"
            show-buttons
            button-layout="horizontal"
            decrement-button-class="p-button-secondary"
            increment-button-class="p-button-secondary"
            increment-button-icon="pi pi-plus"
            decrement-button-icon="pi pi-minus"
            class="w-full"
            placeholder="Sélectionner un jour"
            @update:model-value="updateDay"
          />

          <div v-if="errorMessage" class="error-message">
            <i class="pi pi-exclamation-triangle" />
            {{ errorMessage }}
          </div>

          <div class="help-text">
            <i class="pi pi-info-circle" />
            {{ helpText }}
          </div>
        </div>
      </Transition>

      <div v-if="!isEnabled" class="help-text">
        <i class="pi pi-info-circle" />
        {{ helpText }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.monthly-repeat-selector {
  margin-top: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.toggle-section {
  padding: 0.75rem;
  background-color: var(--surface-50);
  border-radius: 0.5rem;
  border: 1px solid var(--surface-200);
}

.toggle-section label {
  cursor: pointer;
  user-select: none;
}

.day-input-section {
  padding: 1rem;
  background-color: var(--surface-0);
  border-radius: 0.5rem;
  border: 1px solid var(--surface-200);
}

.help-text {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.75rem;
  background-color: var(--blue-50);
  border-left: 3px solid var(--blue-500);
  border-radius: 0.25rem;
  font-size: 0.875rem;
  color: var(--blue-700);
  line-height: 1.4;
}

.help-text i {
  margin-top: 0.125rem;
  flex-shrink: 0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: var(--red-50);
  border-left: 3px solid var(--red-500);
  border-radius: 0.25rem;
  font-size: 0.875rem;
  color: var(--red-700);
}

/* Transition pour l'animation */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
