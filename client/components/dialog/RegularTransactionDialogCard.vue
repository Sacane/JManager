<script setup lang="ts">
import type { RegularTransactionDTO } from '~/types'
import { computed, ref, watch } from 'vue'

interface Props {
  modelValue: boolean
  transaction: RegularTransactionDTO | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', transaction: RegularTransactionDTO): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const formData = ref<RegularTransactionDTO>({
  id: null,
  name: '',
  amount: 0,
  description: '',
  category: '',
  frequency: '',
  startDate: null,
  endDate: null,
  active: true,
})

const originalData = ref<RegularTransactionDTO | null>(null)

const frequencyOptions = [
  { label: 'Quotidien', value: 'DAILY' },
  { label: 'Hebdomadaire', value: 'WEEKLY' },
  { label: 'Mensuel', value: 'MONTHLY' },
  { label: 'Trimestriel', value: 'QUARTERLY' },
  { label: 'Annuel', value: 'YEARLY' },
]

// Détecte si les données ont changé
const hasChanges = computed(() => {
  if (!originalData.value) return false

  return JSON.stringify(formData.value) !== JSON.stringify(originalData.value)
})

// Initialise les données quand le dialog s'ouvre
watch(() => props.transaction, (newTransaction) => {
  if (newTransaction) {
    formData.value = { ...newTransaction }
    originalData.value = { ...newTransaction }
  }
}, { immediate: true, deep: true })

function handleCancel() {
  if (originalData.value) {
    formData.value = { ...originalData.value }
  }
  visible.value = false
}

function handleEdit() {
  if (hasChanges.value) {
    emit('save', { ...formData.value })
    visible.value = false
  }
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="$t('regularTransaction.edit')"
    :style="{ width: '50rem' }"
    :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
  >
    <div class="flex flex-col gap-4 py-4">
      <div class="flex flex-col gap-2">
        <label for="name" class="font-semibold">{{ $t('regularTransaction.name') }}</label>
        <InputText
          id="name"
          v-model="formData.name"
          :placeholder="$t('regularTransaction.name')"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="amount" class="font-semibold">{{ $t('regularTransaction.amount') }}</label>
        <InputNumber
          id="amount"
          v-model="formData.amount"
          mode="currency"
          currency="EUR"
          locale="fr-FR"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="description" class="font-semibold">{{ $t('regularTransaction.description') }}</label>
        <Textarea
          id="description"
          v-model="formData.description"
          rows="3"
          :placeholder="$t('regularTransaction.description')"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="category" class="font-semibold">{{ $t('regularTransaction.category') }}</label>
        <InputText
          id="category"
          v-model="formData.category"
          :placeholder="$t('regularTransaction.category')"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="frequency" class="font-semibold">{{ $t('regularTransaction.frequency') }}</label>
        <Dropdown
          id="frequency"
          v-model="formData.frequency"
          :options="frequencyOptions"
          option-label="label"
          option-value="value"
          :placeholder="$t('regularTransaction.selectFrequency')"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="startDate" class="font-semibold">{{ $t('regularTransaction.startDate') }}</label>
        <Calendar
          id="startDate"
          v-model="formData.startDate"
          date-format="dd/mm/yy"
          :placeholder="$t('regularTransaction.startDate')"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label for="endDate" class="font-semibold">{{ $t('regularTransaction.endDate') }}</label>
        <Calendar
          id="endDate"
          v-model="formData.endDate"
          date-format="dd/mm/yy"
          :placeholder="$t('regularTransaction.endDate')"
        />
      </div>

      <div class="flex items-center gap-2">
        <Checkbox
          id="active"
          v-model="formData.active"
          :binary="true"
        />
        <label for="active" class="font-semibold">{{ $t('regularTransaction.active') }}</label>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button
          :label="$t('common.cancel')"
          severity="secondary"
          @click="handleCancel"
        />
        <Button
          :label="$t('common.edit')"
          :disabled="!hasChanges"
          @click="handleEdit"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
:deep(.p-dialog) {
  max-height: 90vh;
  overflow-y: auto;
}
</style>
