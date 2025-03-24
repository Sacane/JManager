<script setup lang="ts">
import useDate from '~/composables/useDate'

export interface TransactionCreationProps {
  title: string
  digitPlaceholder: number
  transactionPlaceholder: TransactionCreationDTO
  buttonTitle?: string
}

const { title, digitPlaceholder, transactionPlaceholder, buttonTitle } = defineProps<TransactionCreationProps>()
const emit = defineEmits(['visible', 'createTransaction', 'cancelCreation'])
const tag = useTag()
const digits = reactive({
  placeholder: digitPlaceholder,
})
onMounted(() => {
  digits.placeholder = digitPlaceholder
})
const transactionResult = reactive(transactionPlaceholder)
const isVisibleData = ref(false)

const { formattedDateString } = useDate()

const tags = ref([])

onMounted(() => {
  tag.getAllTags().then((tagsResult) => {
    tags.value = tagsResult
  })
})

const jToast = useJToast()

function emitTransaction() {
  if ((digits.placeholder <= 0) || transactionResult.label === '') {
    jToast.warn('Veuillez saisir un montant supérieur à 0')
    return
  }
  const formattedDate = formattedDateString(transactionResult.date)
  const transaction: TransactionCreationDTO = {
    id: transactionResult.id,
    label: transactionResult.label,
    value: digits.placeholder,
    isIncome: transactionResult.isIncome,
    date: formattedDate,
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
</script>

<template>
  <Dialog
    v-model:visible="isVisibleData"
    modal
    :header="title"
    @update:visible="closeDialog"
  >
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé</label>
        <InputText id="label" v-model="transactionResult.label" type="text" autocomplete="off" placeholder="ex: achat meuble leboncoin" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div>
            <RadioButton v-model="transactionResult.isIncome" input-id="selection1" :value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div>
            <RadioButton v-model="transactionResult.isIncome" input-id="selection2" :value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputNumber v-model="transactionResult.value" class="w-full" mode="currency" currency="EUR" :min-fraction-digits="2" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="transactionResult.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <p>Tag</p>
      <Dropdown v-model="transactionResult.tagDTO" label="tag" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <Tag :value="slotTag.option.label" :style="getTagStyle(slotTag.option.colorDTO)" />
        </template>
      </Dropdown>
      <Button :label="buttonTitle ? buttonTitle : 'Créer'" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="emitTransaction" />
    </div>
  </Dialog>
</template>

<style scoped>

</style>
