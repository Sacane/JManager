<script setup lang="ts">
import { getTagStyle } from '~/utils/util'

export interface TransactionCreationProps {
  title: string
  digitPlaceholder: number | null
  transactionPlaceholder: TransactionCreationDTO
  buttonTitle?: string
}

interface InternalTransactionForm {
  id: string | null
  label: string
  value: number | null
  isIncome: boolean
  date: Date
  tagDTO: TagDTO
  isPreview: boolean
}

const { title, digitPlaceholder, transactionPlaceholder, buttonTitle } = defineProps<TransactionCreationProps>()
const emit = defineEmits(['visible', 'createTransaction', 'cancelCreation'])
const tag = useTag()
const digits = reactive({
  placeholder: digitPlaceholder,
})

const transactionResult = reactive<InternalTransactionForm>({
  ...transactionPlaceholder,
  value: transactionPlaceholder.value !== null ? Number.parseFloat(transactionPlaceholder.value) : null,
})
const isVisibleData = ref(false)
const inputNumberRef = ref(null)

const tags = ref<TagDTO[]>([])

onMounted(() => {
  tag.getAllTags().then((tagsResult) => {
    tags.value = tagsResult
  })
  digits.placeholder = digitPlaceholder
})

const jToast = useJToast()

function emitTransaction() {
  if (transactionResult.value === null || (transactionResult.value <= 0) || transactionResult.label === '') {
    jToast.warn('Veuillez saisir un montant supérieur à 0')
    return
  }
  const transaction: TransactionCreationDTO = {
    id: transactionResult.id,
    label: transactionResult.label,
    value: transactionResult.value !== null ? transactionResult.value.toFixed(2) : null,
    isIncome: transactionResult.isIncome,
    date: transactionResult.date,
    tagDTO: transactionResult.tagDTO,
    isPreview: transactionResult.isPreview,
  }
  emit('createTransaction', transaction)
}
function closeDialog() {
  emit('visible', false)
  emit('cancelCreation')
  isVisibleData.value = false
}
function handleTabKey(event: KeyboardEvent) {
  if (event.key === 'Tab') {
    event.preventDefault()
    const input = inputNumberRef.value?.$el.querySelector('input')
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
    dismissable-mask
    modal
    :header="title"
    :style="{ width: '30rem' }"
    @update:visible="closeDialog"
    @keydown.enter="emitTransaction"
  >
    <div class="h-full mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé</label>
        <InputText id="label" v-model="transactionResult.label" type="text" autocomplete="off" placeholder="ex: achat meuble leboncoin" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div class="flex gap-1">
            <RadioButton v-model="transactionResult.isIncome" input-id="selection1" :value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div class="flex gap-1">
            <RadioButton v-model="transactionResult.isIncome" input-id="selection2" :value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputNumber ref="inputNumberRef" v-model="transactionResult.value" aria-placeholder="" placeholder="0,00" class="w-full inputNumber" :max-fraction-digits="2" :min-fraction-digits="2" :formatter="(value: number) => value ? value.toFixed(2) : ''" @keydown="handleTabKey" />
      </div>
      <div class="flex flex-col gap-3 w-50%">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <DatePicker id="calendar" v-model="transactionResult.date" panel-class="min-w-min w-12rem" :first-day-of-week="1" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <p>Tag</p>
      <Select v-model="transactionResult.tagDTO" label="tag" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <Tag :value="slotTag.option.label" :style="getTagStyle(slotTag.option.colorDTO)" />
        </template>
      </Select>
      <div class="flex flex-row gap-5">
        <Button severity="secondary" label="Annuler" class="mt-6 w-full text-white" @click="closeDialog" />
        <Button ref="validerButtonRef" :label="buttonTitle ? buttonTitle : 'Créer'" class="mt-6 w-full btn-primary text-white" @click="emitTransaction" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.inputNumber ::placeholder {
  color: grey;
  opacity: 0.8;
}
</style>
