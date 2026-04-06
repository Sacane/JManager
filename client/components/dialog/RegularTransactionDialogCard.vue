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
  (e: 'delete', transactionId: string): void
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

const formData = ref<RegularTransactionDTO>({
  id: '',
  label: '',
  value: 0,
  isIncome: false,
  regularity: '',
  startDate: '' as string | Date,
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
  bookletIds: [],
})

const originalData = ref<string>('')

const frequencyOptions = [
  /* { label: 'Quotidien', value: 'DAILY', icon: 'pi pi-calendar' },
  { label: 'Hebdomadaire', value: 'WEEKLY', icon: 'pi pi-calendar' }, */
  { label: 'Mensuel', value: 'MONTHLY', icon: 'pi pi-calendar' },
  /* { label: 'Trimestriel', value: 'QUARTERLY', icon: 'pi pi-calendar' },
  { label: 'Annuel', value: 'YEARLY', icon: 'pi pi-calendar' }, */
]

const frequencyTypeOptions = [
  { label: 'Pour toujours', value: 'FOREVER', icon: 'pi pi-infinity' },
  { label: 'Jusqu\'à une date', value: 'UNTIL_DATE', icon: 'pi pi-calendar-times' },
  { label: 'Nombre de fois', value: 'TIMES', icon: 'pi pi-hashtag' },
]

const startDateValue = computed({
  get: () => formData.value.startDate ? new Date(formData.value.startDate) : null,
  set: (value: Date | null) => {
    formData.value.startDate = (value ? value.toISOString().split('T')[0] : '') as string | Date
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
      bookletIds: newTransaction.bookletIds || [],
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

function handleDelete() {
  if (formData.value.id) {
    emit('delete', formData.value.id)
    console.warn('Transaction régulière supprimée avec succès')
  }
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="formData.isIncome ? '💰 Transaction régulière - Recette' : '💸 Transaction régulière - Dépense'"
    :style="{ width: '50rem' }"
    :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    :draggable="false"
  >
    <div v-if="loading" class="flex justify-center items-center py-8">
      <ProgressSpinner />
    </div>

    <template v-else-if="transaction">
      <div class="flex flex-col gap-5 py-4">
        <div class="section-card">
          <div class="section-header">
            <i class="pi pi-info-circle" />
            <h3>Informations générales</h3>
          </div>
          <div class="section-content">
            <div class="form-field">
              <label for="label" class="field-label">
                <i class="pi pi-tag" />
                Libellé
              </label>
              <InputText
                id="label"
                v-model="formData.label"
                placeholder="Ex: Salaire, Loyer, Abonnement..."
                class="w-full"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="form-field">
                <label for="value" class="field-label">
                  <i class="pi pi-euro" />
                  Montant
                </label>
                <InputNumber
                  id="value"
                  v-model="formData.value"
                  mode="currency"
                  currency="EUR"
                  locale="fr-FR"
                  class="w-full"
                />
              </div>

              <div class="form-field">
                <label class="field-label">
                  <i class="pi pi-arrow-right-arrow-left" />
                  Type de transaction
                </label>
                <div class="flex items-center gap-4 h-[42px]">
                  <div class="flex items-center gap-2">
                    <RadioButton
                      id="expense"
                      v-model="formData.isIncome"
                      name="transactionType"
                      :value="false"
                    />
                    <label for="expense" class="cursor-pointer">
                      <Tag value="Dépense" severity="danger" icon="pi pi-arrow-down" />
                    </label>
                  </div>
                  <div class="flex items-center gap-2">
                    <RadioButton
                      id="income"
                      v-model="formData.isIncome"
                      name="transactionType"
                      :value="true"
                    />
                    <label for="income" class="cursor-pointer">
                      <Tag value="Recette" severity="success" icon="pi pi-arrow-up" />
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="section-header">
            <i class="pi pi-clock" />
            <h3>Fréquence et dates</h3>
          </div>
          <div class="section-content">
            <div class="grid grid-cols-2 gap-4">
              <div class="form-field">
                <label for="regularity" class="field-label">
                  <i class="pi pi-sync" />
                  Fréquence
                </label>
                <Select
                  id="regularity"
                  v-model="formData.regularity"
                  :options="frequencyOptions"
                  option-label="label"
                  option-value="value"
                  placeholder="Sélectionner une fréquence"
                  class="w-full"
                />
              </div>

              <div class="form-field">
                <label for="startDate" class="field-label">
                  <i class="pi pi-calendar-plus" />
                  Date de début
                </label>
                <DatePicker
                  id="startDate"
                  v-model="startDateValue"
                  date-format="dd/mm/yy"
                  placeholder="Sélectionner une date"
                  class="w-full"
                  show-icon
                />
              </div>
            </div>

            <div class="form-field">
              <label for="frequencyType" class="field-label">
                <i class="pi pi-replay" />
                Durée de récurrence
              </label>
              <Select
                id="frequencyType"
                v-model="formData.frequencyProperty.type"
                :options="frequencyTypeOptions"
                option-label="label"
                option-value="value"
                placeholder="Sélectionner un type"
                class="w-full"
              />
            </div>

            <Transition name="fade">
              <div v-if="formData.frequencyProperty.type === 'UNTIL_DATE'" class="form-field">
                <label for="untilDate" class="field-label">
                  <i class="pi pi-calendar-times" />
                  Date de fin
                </label>
                <DatePicker
                  id="untilDate"
                  v-model="untilDateValue"
                  date-format="dd/mm/yy"
                  placeholder="Sélectionner une date de fin"
                  class="w-full"
                  show-icon
                />
              </div>
            </Transition>

            <Transition name="fade">
              <div v-if="formData.frequencyProperty.type === 'TIMES'" class="form-field">
                <label for="times" class="field-label">
                  <i class="pi pi-hashtag" />
                  Nombre d'occurrences
                </label>
                <InputNumber
                  id="times"
                  v-model="formData.frequencyProperty.times"
                  :min="1"
                  placeholder="Nombre de fois"
                  show-buttons
                  class="w-full"
                />
              </div>
            </Transition>
          </div>
        </div>

        <div class="section-card">
          <div class="section-header">
            <i class="pi pi-bookmark" />
            <h3>Catégorie</h3>
          </div>
          <div class="section-content">
            <div class="form-field">
              <label for="tag" class="field-label">
                <i class="pi pi-tags" />
                Tag associé
              </label>
              <Select
                id="tag"
                v-model="formData.tagDTO"
                :options="tags"
                option-label="label"
                placeholder="Sélectionner un tag"
                class="w-full"
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
              </Select>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between items-center w-full">
        <Button
          label="Supprimer"
          icon="pi pi-trash"
          severity="danger"
          :disabled="loading"
          outlined
          @click="handleDelete"
        />
        <div class="flex gap-3">
          <Button
            label="Annuler"
            icon="pi pi-times"
            severity="secondary"
            :disabled="loading"
            outlined
            @click="handleCancel"
          />
          <Button
            label="Enregistrer"
            icon="pi pi-check"
            :disabled="!hasChanges || loading"
            @click="handleEdit"
          />
        </div>
      </div>
    </template>
  </Dialog>
</template>

<style scoped lang="scss">
:deep(.p-dialog) {
  max-height: 90vh;
  overflow-y: auto;
}

.section-card {
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface-card);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--surface-50);
  border-bottom: 1px solid var(--surface-border);

  i {
    color: var(--primary-color);
    font-size: 1.1rem;
  }

  h3 {
    margin: 0;
    font-size: 1rem;
    font-weight: 600;
    color: var(--text-color);
  }
}

.section-content {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-color-secondary);

  i {
    font-size: 0.875rem;
    color: var(--primary-color);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

:deep(.p-inputtext),
:deep(.p-dropdown),
:deep(.p-calendar),
:deep(.p-inputnumber) {
  &:focus,
  &:focus-within {
    box-shadow: 0 0 0 0.2rem var(--primary-color-alpha);
  }
}

:deep(.p-radiobutton) {
  .p-radiobutton-box {
    border-width: 2px;
  }
}

.grid {
  display: grid;

  &.grid-cols-2 {
    grid-template-columns: repeat(2, 1fr);

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }
}

.gap-4 {
  gap: 1rem;
}
</style>
