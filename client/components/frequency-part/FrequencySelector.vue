<script setup lang="ts">
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import { computed, ref, watch } from 'vue'
import useDate from '~/composables/useDate'

export type FrequencyPropertyType = 'FOREVER' | 'UNTIL_DATE' | 'TIMES'

export interface FrequencyPropertyDTOClient {
  type: FrequencyPropertyType
  untilDate: Date | null
  times: number | null
}

const props = defineProps<{
  modelValue: FrequencyPropertyDTOClient
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FrequencyPropertyDTOClient): void
}>()

const frequencyTypes: FrequencyPropertyType[] = ['FOREVER', 'UNTIL_DATE', 'TIMES']

const errors = ref<{ untilDate?: string, times?: string }>({})
const { formattedDateString } = useDate()
const isUntilDate = computed(() => props.modelValue.type === 'UNTIL_DATE')
const isTimes = computed(() => props.modelValue.type === 'TIMES')

function updateField<K extends keyof FrequencyPropertyDTOClient>(key: K, value: FrequencyPropertyDTOClient[K]) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
  errors.value = {}
}

watch(() => props.modelValue.type, (newType) => {
  if (newType === 'FOREVER') {
    updateField('untilDate', null)
    updateField('times', null)
  } else if (newType === 'UNTIL_DATE') {
    updateField('times', null)
  } else if (newType === 'TIMES') {
    updateField('untilDate', null)
  }
})
function displayableType(type: FrequencyPropertyType): string {
  switch (type) {
    case 'FOREVER': return 'Répéter toujours'
    case 'TIMES': return 'Définir un nombre de répétition'
    case 'UNTIL_DATE': return 'Sélectionner une date de fin'
  }
}
</script>

<template>
  <form class="max-w-125 mx-auto" @submit.prevent>
    <div class="mb-6">
      <h3 class="mb-2 font-medium">
        Type de fréquence
      </h3>
      <Select
        :model-value="props.modelValue.type"
        :options="frequencyTypes"
        :option-label="displayableType"
        placeholder="Sélectionner le type"
        class="w-full"
        @update:model-value="updateField('type', $event)"
      />
    </div>

    <div v-if="isUntilDate" class="mb-6">
      <label for="untilDate" class="block mb-2 font-medium">Date de fin</label>
      <DatePicker
        id="untilDate"
        v-model="props.modelValue.untilDate"
        date-format="dd/mm/yy"
        :show-icon="true"
        :min-date="new Date()"
        class="w-full"
        @update:model-value="updateField('untilDate', formattedDateString(props.modelValue.untilDate))"
      />
      <span v-if="errors.untilDate" class="text-red-500 text-sm mt-1 block">{{ errors.untilDate }}</span>
    </div>

    <div v-if="isTimes" class="mb-6">
      <label for="times" class="block mb-2 font-medium">Nombre de répétitions</label>
      <InputNumber
        id="times"
        v-model="props.modelValue.times"
        :min="1"
        :step="1"
        show-buttons
        button-layout="horizontal"
        decrement-button-class="p-button-secondary"
        increment-button-class="p-button-secondary"
        increment-button-icon="pi pi-plus"
        decrement-button-icon="pi pi-minus"
        class="w-full"
        @update:model-value="updateField('times', $event)"
      />
      <span v-if="errors.times" class="text-red-500 text-sm mt-1 block">{{ errors.times }}</span>
    </div>
  </form>
</template>
