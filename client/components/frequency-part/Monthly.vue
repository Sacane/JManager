<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type FrequencyPropertyType = 'FOREVER' | 'UNTIL_DATE' | 'TIMES'

interface FrequencyPropertyDTOClient {
  type: FrequencyPropertyType
  untilDate: string | null
  times: number | null
}

const props = defineProps<{
  initial?: Partial<FrequencyPropertyDTOClient>
}>()

const emit = defineEmits<{
  (e: 'update:frequencyProperty', payload: FrequencyPropertyDTOClient): void
}>()

const frequencyTypes: FrequencyPropertyType[] = ['FOREVER', 'UNTIL_DATE', 'TIMES']

const form = ref<FrequencyPropertyDTOClient>({
  type: (props.initial?.type ?? 'FOREVER') as FrequencyPropertyType,
  untilDate: props.initial?.untilDate ?? null,
  times: props.initial?.times ?? null,
})

const errors = ref<{ untilDate?: string, times?: string }>({})

const isUntilDate = computed(() => form.value.type === 'UNTIL_DATE')
const isTimes = computed(() => form.value.type === 'TIMES')

watch(() => form.value.type, (newType) => {
  if (newType === 'FOREVER') {
    form.value.untilDate = null
    form.value.times = null
  } else if (newType === 'UNTIL_DATE') {
    form.value.times = null
  } else if (newType === 'TIMES') {
    form.value.untilDate = null
  }
  errors.value = {}
})

function validate(): boolean {
  errors.value = {}

  if (form.value.type === 'UNTIL_DATE' && !form.value.untilDate) {
    errors.value.untilDate = 'La date de fin est requise'
    return false
  }

  if (form.value.type === 'TIMES') {
    if (!form.value.times || form.value.times <= 0) {
      errors.value.times = 'Le nombre de répétitions doit être supérieur à 0'
      return false
    }
  }

  return true
}

function handleSubmit() {
  if (!validate()) return

  emit('update:frequencyProperty', {
    type: form.value.type,
    untilDate: form.value.untilDate,
    times: form.value.times,
  })
}

function displayableType(type: FrequencyPropertyType): string {
  switch (type) {
    case 'FOREVER': return 'Répéter toujours'
    case 'TIMES': return 'Définir un nombre de répétition'
    case 'UNTIL_DATE': return 'Sélectionner une date de fin'
  }
}
</script>

<template>
  <form class="freq-form" @submit.prevent="handleSubmit">
    <div class="form-group">
      <h3>Type de fréquence</h3>
      <Dropdown
        v-model="form.type"
        :options="frequencyTypes"
        :option-label="displayableType"
        placeholder="Sélectionner le type"
        class="w-full md:w-20rem"
      />
    </div>

    <div v-if="isUntilDate" class="form-group">
      <label for="untilDate">Date de fin</label>
      <input
        id="untilDate"
        v-model="form.untilDate"
        type="date"
        class="input"
      >
      <span v-if="errors.untilDate" class="error">{{ errors.untilDate }}</span>
    </div>

    <div v-if="isTimes" class="form-group">
      <label for="times">Nombre de répétitions</label>
      <input
        id="times"
        v-model="form.times"
        type="number"
        min="1"
        step="1"
        class="input"
      >
      <span v-if="errors.times" class="error">{{ errors.times }}</span>
    </div>

    <div class="actions">
      <button type="submit" class="btn-submit">
        Valider
      </button>
      <button
        type="button"
        class="btn-reset"
        @click="form.type = 'FOREVER'"
      >
        Réinitialiser
      </button>
    </div>
  </form>
</template>

<style scoped lang="scss">
.freq-form {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  max-width: 480px;
  width: 100%;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 1.5rem;
  width: 100%;
  h3 {
    font-size: 1rem;
    margin-bottom: 1rem;
    color: #333;
  }

  label {
    display: block;
    margin-bottom: 0.5rem;
    color: #555;
    font-size: 0.9rem;
  }
}

.radio-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #f8fafc;
  }

  input[type="radio"] {
    margin: 0;
  }

  .radio-text {
    font-size: 0.9rem;
    color: #444;
  }
}

.input {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.9rem;

  &:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
}

.error {
  color: #dc2626;
  font-size: 0.8rem;
  margin-top: 0.25rem;
  display: block;
}

.actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
}

.btn-submit {
  padding: 0.6rem 1.2rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #2563eb;
  }
}

.btn-reset {
  padding: 0.6rem 1.2rem;
  background: transparent;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #f8fafc;
    border-color: #cbd5e1;
  }
}
</style>
