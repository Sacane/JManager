<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getTagStyle } from '~/utils/util'

interface Props {
  modelValue: boolean
  transaction: RegularTransactionDTO | null
  loading?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', transaction: RegularTransactionDTO): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<Emits>()

const { getAllTags } = useTag()
const tags = ref<TagDTO[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

// Initialisation avec tous les champs de RegularTransactionDTO
const formData = ref<RegularTransactionDTO>({
  id: '',
  label: '',
  value: 0,
  isIncome: false,
  regularity: '',
  startDate: '',
  frequencyProperty: {
    type: 'FOREVER',
    untilDate: undefined,
    times: undefined,
  },
  tagDTO: {
    tagId: undefined,
    label: '',
    colorDTO: {
      red: 0,
      green: 0,
      blue: 0,
    },
    isDefault: false,
  },
})

const originalData = ref<string>('')

const frequencyOptions = [
  { label: 'Quotidien', value: 'DAILY' },
  { label: 'Hebdomadaire', value: 'WEEKLY' },
  { label: 'Mensuel', value: 'MONTHLY' },
  { label: 'Trimestriel', value: 'QUARTERLY' },
  { label: 'Annuel', value: 'YEARLY' },
]

const frequencyTypeOptions = [
  { label: 'Pour toujours', value: 'FOREVER' },
  { label: 'Jusqu\'à une date', value: 'UNTIL_DATE' },
  { label: 'Nombre de fois', value: 'TIMES' },
]

// Computed pour gérer la conversion des dates
const startDateValue = computed({
  get: () => formData.value.startDate ? new Date(formData.value.startDate) : null,
  set: (value) => {
    formData.value.startDate = value ? value.toISOString().split('T')[0] : ''
  },
})

const untilDateValue = computed({
  get: () => formData.value.frequencyProperty.untilDate ? new Date(formData.value.frequencyProperty.untilDate) : null,
  set: (value) => {
    formData.value.frequencyProperty.untilDate = value ? value.toISOString().split('T')[0] : undefined
  },
})

const hasChanges = computed(() => {
  if (!originalData.value) return false
  return JSON.stringify(formData.value) !== originalData.value
})

watch(() => props.transaction, (newTransaction) => {
  if (newTransaction) {
    formData.value = {
      id: newTransaction.id,
      label: newTransaction.label || '',
      value: newTransaction.value || 0,
      isIncome: newTransaction.isIncome || false,
      regularity: newTransaction.regularity || '',
      startDate: newTransaction.startDate || '',
      frequencyProperty: newTransaction.frequencyProperty || {
        type: 'FOREVER',
        untilDate: undefined,
        times: undefined,
      },
      tagDTO: newTransaction.tagDTO || {
        tagId: undefined,
        label: '',
        colorDTO: {
          red: 0,
          green: 0,
          blue: 0,
        },
        isDefault: false,
      },
    }
    originalData.value = JSON.stringify(formData.value)
  }
}, { immediate: true, deep: true })

onMounted(() => {
  getAllTags()
    .then((res) => {
      tags.value = res
    })
    .catch((err) => {
      console.error('Erreur lors de la récupération des tags:', err)
    })
})

function handleCancel() {
  if (originalData.value) {
    formData.value = JSON.parse(originalData.value)
  }
  visible.value = false
}

function handleEdit() {
  if (hasChanges.value) {
    emit('save', { ...formData.value })
  }
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="Modifier la transaction régulière"
    :style="{ width: '50rem' }"
    :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
  >
    <div v-if="loading" class="flex justify-center items-center py-8">
      <ProgressSpinner />
    </div>

    <template v-else-if="transaction">
      <div class="flex flex-col gap-4 py-4">
        <div class="flex flex-col gap-2">
          <label for="label" class="font-semibold">Libellé</label>
          <InputText
            id="label"
            v-model="formData.label"
            placeholder="Libellé de la transaction"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="value" class="font-semibold">Montant</label>
          <InputNumber
            id="value"
            v-model="formData.value"
            mode="currency"
            currency="EUR"
            locale="fr-FR"
          />
        </div>

        <div class="flex items-center gap-2">
          <Checkbox
            id="isIncome"
            v-model="formData.isIncome"
            :binary="true"
          />
          <label for="isIncome" class="font-semibold">Revenu</label>
        </div>

        <div class="flex flex-col gap-2">
          <label for="regularity" class="font-semibold">Fréquence</label>
          <Dropdown
            id="regularity"
            v-model="formData.regularity"
            :options="frequencyOptions"
            option-label="label"
            option-value="value"
            placeholder="Sélectionner une fréquence"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="startDate" class="font-semibold">Date de début</label>
          <Calendar
            id="startDate"
            v-model="startDateValue"
            date-format="dd/mm/yy"
            placeholder="Date de début"
          />
        </div>

        <!-- Section FrequencyProperty -->
        <div class="flex flex-col gap-4 p-4 border rounded-lg bg-gray-50">
          <h3 class="font-semibold text-lg">
            Propriétés de la fréquence
          </h3>

          <div class="flex flex-col gap-2">
            <label for="frequencyType" class="font-semibold">Type de fréquence</label>
            <Dropdown
              id="frequencyType"
              v-model="formData.frequencyProperty.type"
              :options="frequencyTypeOptions"
              option-label="label"
              option-value="value"
              placeholder="Sélectionner un type"
            />
          </div>

          <template v-if="formData.frequencyProperty.type === 'UNTIL_DATE'">
            <div class="flex flex-col gap-2">
              <label for="untilDate" class="font-semibold">Jusqu'au</label>
              <Calendar
                id="untilDate"
                v-model="untilDateValue"
                date-format="dd/mm/yy"
                placeholder="Date de fin"
              />
            </div>
          </template>

          <template v-if="formData.frequencyProperty.type === 'TIMES'">
            <div class="flex flex-col gap-2">
              <label for="times" class="font-semibold">Nombre d'occurrences</label>
              <InputNumber
                id="times"
                v-model="formData.frequencyProperty.times"
                :min="1"
                placeholder="Nombre de fois"
                show-buttons
              />
            </div>
          </template>
        </div>

        <div class="flex flex-col gap-2">
          <label for="tag" class="font-semibold">Tag</label>
          <Dropdown
            id="tag"
            v-model="formData.tagDTO"
            :options="tags"
            option-label="label"
            placeholder="Sélectionner un tag"
          >
            <template #value="slotProps">
              <Tag
                v-if="slotProps.value"
                :value="slotProps.value.label"
                :style="getTagStyle(slotProps.value.colorDTO)"
              />
            </template>
            <template #option="slotProps">
              <Tag
                :value="slotProps.option.label"
                :style="getTagStyle(slotProps.option.colorDTO)"
              />
            </template>
          </Dropdown>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button
          label="Annuler"
          severity="secondary"
          :disabled="loading"
          @click="handleCancel"
        />
        <Button
          label="Modifier"
          :disabled="!hasChanges || loading"
          @click="handleEdit"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped lang="scss">
:deep(.p-dialog) {
  max-height: 90vh;
  overflow-y: auto;
}

.bg-gray-50 {
  background-color: rgba(0, 0, 0, 0.02);
}
</style>
