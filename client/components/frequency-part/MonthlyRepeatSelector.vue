<script setup lang="ts">
import InputNumber from 'primevue/inputnumber'
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  repeatDay: number | null
}>()

const emit = defineEmits<{
  (e: 'update:repeatDay', value: number | null): void
}>()

const isEnabled = ref(props.repeatDay !== null)
const selectedDay = ref(props?.repeatDay ?? 1)

const errorMessage = ref<string | null>(null)

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

function toggleEnabled(enabled: boolean) {
  isEnabled.value = enabled

  if (enabled) {
    emit('update:repeatDay', selectedDay.value)
  } else {
    emit('update:repeatDay', null)
  }
}

watch(() => selectedDay, (newValue) => {
  if (newValue === null) {
    isEnabled.value = false
  } else {
    isEnabled.value = true
    selectedDay.value = newValue.value
    emit('update:repeatDay', newValue.value)
  }
})

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
  <div class="mt-4">
    <div class="flex flex-col gap-4">
      <div class="p-3 bg-gray-500 rounded-lg border border-gray-200">
        <div class="flex items-center gap-2">
          <ToggleSwitch
            id="enableDaySelection"
            :model-value="isEnabled"
            @update:model-value="toggleEnabled"
          />
          <label for="enableDaySelection" class="font-medium cursor-pointer select-none">
            Définir un jour spécifique du mois
          </label>
        </div>
      </div>

      <Transition name="fade">
        <div v-if="isEnabled" class="p-4 rounded-lg border border-gray-200">
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

          <div v-if="errorMessage" class="flex items-center gap-2 mt-2 p-2 bg-red-50 border-l-3 border-red-500 rounded text-sm text-red-700">
            <i class="pi pi-exclamation-triangle" />
            {{ errorMessage }}
          </div>

          <div class="flex items-start gap-2 mt-3 p-3 bg-blue-50 border-l-3 border-blue-500 rounded text-sm text-blue-700 leading-relaxed">
            <i class="pi pi-info-circle mt-0.5 flex-shrink-0" />
            {{ helpText }}
          </div>
        </div>
      </Transition>

      <div v-if="!isEnabled" class="flex items-start gap-2 mt-3 p-3 bg-blue-50 border-l-3 border-blue-500 rounded text-sm text-blue-700 leading-relaxed">
        <i class="pi pi-info-circle mt-0.5 flex-shrink-0" />
        {{ helpText }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
