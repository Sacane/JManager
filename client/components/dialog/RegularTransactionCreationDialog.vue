<script setup lang="ts">
import type { FrequencyPropertyDTOClient, FrequencyPropertyType } from '~/components/frequency-part/FrequencySelector.vue'
import useDate from '~/composables/useDate'
import { getTagStyle } from '~/utils/util'

defineProps<{
  booklets: OnlyBookletInfo[]
  loading?: boolean
}>()
const emit = defineEmits(['visible', 'createTransaction', 'cancelCreation'])
const tag = useTag()
const tags = ref<TagDTO[]>([])
const { formattedDateString, frequencyToString, strToFrequency } = useDate()

interface RegularTrFormType {
  label: string
  amount: number | undefined
  date: Date
  frequency: string
  monthlyFrequency: {
    type: FrequencyPropertyType
    untilDate: Date | undefined
    times: number | undefined
  }
  repeatDay: number | null
  isIncome: boolean
  tagDTO: TagDTO
  selectedBooklets: OnlyBookletInfo[]
}

const regularTrForm: RegularTrFormType = reactive({
  label: '',
  amount: undefined,
  date: new Date(),
  frequency: frequencyToString('MONTHLY'),
  monthlyFrequency: {
    type: 'FOREVER' as FrequencyPropertyType,
    untilDate: undefined,
    times: undefined,
  },
  repeatDay: null,
  isIncome: false,
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
  selectedBooklets: [],
})

onMounted(() => {
  tag.getAllTags().then((tagsResult) => {
    tags.value = tagsResult
    if (tagsResult.length > 0) {
      regularTrForm.tagDTO = tagsResult[0]
    }
  })
})

const fieldErrors = reactive<{ label: string | null, amount: string | null, booklets: string | null }>({
  label: null,
  amount: null,
  booklets: null,
})

function labelErrorFor(value: string): string | null {
  return value.trim() === '' ? 'Indiquez un libellé' : null
}

function amountErrorFor(value: number | undefined | null): string | null {
  if (value === undefined || value === null) return 'Indiquez un montant'
  if (value <= 0) return 'Le montant doit être supérieur à 0'
  return null
}

function bookletsErrorFor(selected: OnlyBookletInfo[]): string | null {
  return selected.length === 0 ? 'Choisissez au moins un livret' : null
}

watch(() => regularTrForm.label, (label) => {
  if (fieldErrors.label) fieldErrors.label = labelErrorFor(label)
})

watch(() => regularTrForm.amount, (amount) => {
  if (fieldErrors.amount) fieldErrors.amount = amountErrorFor(amount)
})

watch(() => regularTrForm.selectedBooklets, (selected) => {
  if (fieldErrors.booklets) fieldErrors.booklets = bookletsErrorFor(selected)
})

function emitTransaction() {
  fieldErrors.label = labelErrorFor(regularTrForm.label)
  fieldErrors.amount = amountErrorFor(regularTrForm.amount)
  fieldErrors.booklets = bookletsErrorFor(regularTrForm.selectedBooklets)

  // These guards used to return without a word: a click on "Créer" with an empty label did nothing
  // at all, which is indistinguishable from a broken button.
  if (fieldErrors.label || fieldErrors.amount || fieldErrors.booklets) return
  const frequency = strToFrequency(regularTrForm.frequency)
  if (frequency === 'MONTHLY') {
    const formattedStartDate = formattedDateString(regularTrForm.date)
    const regularTransactionCreationRequest: MonthlyTransactionCreationRequest = {
      label: regularTrForm.label,
      value: regularTrForm.amount,
      isIncome: regularTrForm.isIncome,
      startDate: formattedStartDate as any as Date,
      tagDTO: regularTrForm.tagDTO,
      frequencyProperty: regularTrForm.monthlyFrequency as any,
      repeatDay: regularTrForm.repeatDay,
      bookletIds: regularTrForm.selectedBooklets.map(b => String(b.id)),
    }
    emit('createTransaction', regularTransactionCreationRequest)
    regularTrForm.label = ''
    regularTrForm.amount = undefined
    regularTrForm.date = new Date()
    regularTrForm.frequency = frequencyToString('MONTHLY')
    regularTrForm.monthlyFrequency = {
      type: 'FOREVER' as FrequencyPropertyType,
      untilDate: undefined,
      times: undefined,
    }
    regularTrForm.selectedBooklets = []
  }
}

const isVisibleData = ref(false)
const inputNumberRef = ref(null)

function closeDialog() {
  emit('visible', false)
  emit('cancelCreation')
  isVisibleData.value = false
}

function updateMonthlyFrequencyValue(value: FrequencyPropertyDTOClient) {
  regularTrForm.monthlyFrequency = value as any
}
function updateMonthlyRepeatValue(value: number | null) {
  regularTrForm.repeatDay = value
}

function handleTabKey(event: KeyboardEvent) {
  if (event.key === 'Tab') {
    event.preventDefault()
    const input = (inputNumberRef.value as any)?.$el?.querySelector('input')
    if (input && input.value.includes(',')) {
      const cursorPosition = input.selectionStart
      const decimalPosition = input.value.indexOf(',')
      if (cursorPosition <= decimalPosition) {
        input.setSelectionRange(decimalPosition + 1, decimalPosition + 1)
      } else {
        const nextInput = input.nextElementSibling
        if (nextInput) {
          nextInput.focus()
        }
      }
    }
  }
}
</script>

<template>
  <Dialog
    v-model:visible="isVisibleData"
    :dismissable-mask="!loading"
    :closable="!loading"
    :close-on-escape="!loading"
    modal
    header="Créer une transaction régulière"
    :style="{ width: '35rem' }"
    @update:visible="closeDialog"
    @keydown.enter="!loading && emitTransaction()"
  >
    <div v-if="loading" class="h-14rem flex flex-col items-center justify-center gap-3 text-[var(--text-secondary)]">
      <i class="pi pi-spin pi-spinner text-3xl" />
      <span>Création en cours...</span>
    </div>
    <div v-else class="h-full mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé</label>
        <InputText id="label" v-model="regularTrForm.label" type="text" autocomplete="off" placeholder="ex: achat meuble leboncoin" maxlength="100" :invalid="!!fieldErrors.label" aria-describedby="rt-label-error" />
        <FieldError id="rt-label-error" data-test="error-rt-label" :message="fieldErrors.label" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Sélectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div class="flex gap-1">
            <RadioButton v-model="regularTrForm.isIncome" input-id="selection1" :value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div class="flex gap-1">
            <RadioButton v-model="regularTrForm.isIncome" input-id="selection2" :value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputNumber ref="inputNumberRef" v-model="regularTrForm.amount" aria-placeholder="" placeholder="0,00" class="w-full inputNumber" :max-fraction-digits="2" :min-fraction-digits="2" :invalid="!!fieldErrors.amount" aria-describedby="rt-amount-error" :formatter="(value: number) => value ? value.toFixed(2) : ''" @keydown="handleTabKey" />
      </div>
      <FieldError id="rt-amount-error" data-test="error-rt-amount" :message="fieldErrors.amount" />
      <div class="flex flex-col gap-3 w-50%">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <DatePicker id="calendar" v-model="regularTrForm.date" panel-class="min-w-min w-12rem" :first-day-of-week="1" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <div class="flex flex-row gap-5">
        <div class="flex flex-col">
          <p>Tag</p>
          <Select v-model="regularTrForm.tagDTO" label="tag" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
            <template #option="slotTag">
              <Tag :value="slotTag.option.label" :style="getTagStyle(slotTag.option.colorDTO)" />
            </template>
          </Select>
        </div>
        <div class="flex flex-col">
          <p>Fréquence</p>
          <Select v-model="regularTrForm.frequency" :options="[frequencyToString('WEEKLY'), frequencyToString('MONTHLY'), frequencyToString('YEARLY')]" placeholder="Répéter" class="w-full md:w-14rem" />
        </div>
      </div>
      <div class="flex flex-col gap-3 mt-4">
        <label for="booklets" class="block text-sm font-medium text-gray-700">Livrets associés</label>
        <MultiSelect
          id="booklets"
          v-model="regularTrForm.selectedBooklets"
          :options="booklets"
          option-label="label"
          placeholder="Sélectionner un ou plusieurs livrets"
          class="w-full"
          display="chip"
        >
          <template #option="slotProps">
            <div class="flex items-center gap-2">
              <span>{{ slotProps.option.label }}</span>
            </div>
          </template>
        </MultiSelect>
        <FieldError data-test="error-rt-booklets" :message="fieldErrors.booklets" />
        <small class="text-gray-500">La transaction sera appliquée aux livrets sélectionnés</small>
      </div>
      <div v-if="regularTrForm.frequency === frequencyToString('MONTHLY')" class="flex flex-col gap-3">
        <FrequencySelector
          :model-value="regularTrForm.monthlyFrequency"
          @update:model-value="value => updateMonthlyFrequencyValue(value)"
        />
        <MonthlyRepeatSelector
          :repeat-day="regularTrForm.repeatDay"
          @update:repeat-day="value => updateMonthlyRepeatValue(value)"
        />
      </div>
      <div class="flex flex-row gap-5">
        <Button data-test="cancel-btn" severity="secondary" label="Annuler" class="mt-6 w-full text-white" @click="closeDialog" />
        <Button data-test="create-btn" label="Créer" class="mt-6 w-full btn-primary text-white" @click="emitTransaction" />
      </div>
    </div>
  </Dialog>
</template>

<style lang="scss" scoped>

</style>
