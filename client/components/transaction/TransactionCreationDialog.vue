<script setup lang="ts">
export interface TransactionCreationProps {
  title: string
  integerpart: string
  decimalpart: string
  transactionPlaceholder: TransactionCreationDTO
}
const { title, integerpart, decimalpart, transactionPlaceholder } = defineProps<TransactionCreationProps>()
const emit = defineEmits(['visible', 'createTransaction', 'cancel'])
const tag = useTag()
const digits = reactive({
  integerpart,
  decimalpart,
})
const transactionResult = reactive(transactionPlaceholder)
const isVisibleData = ref(false)

const tags = ref([])

onMounted(() => {
  tag.getAllTags().then((tagsResult) => {
    tags.value = tagsResult
  })
})

function emitTransaction() {
  if ((digits.integerpart === '0' && digits.decimalpart === '0') || transactionResult.label === '') {
    return
  }
  const amount = `${digits.integerpart}.${digits.decimalpart}`
  const transaction: TransactionCreationDTO = {
    label: transactionResult.label,
    value: amount,
    isIncome: transactionResult.isIncome,
    date: transactionResult.date,
    tagDTO: transactionResult.tagDTO,
    isPreview: transactionResult.isPreview,
  }
  emit('createTransaction', transaction)
}
function closeDialog() {
  emit('visible', false)
  emit('cancel')
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
        <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
        <InputText id="label" v-model="transactionResult.label" type="text" autocomplete="off" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div>
            <RadioButton v-model="transactionResult.isIncome" input-id="selection1" value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div>
            <RadioButton v-model="transactionResult.isIncome" input-id="selection2" value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="digits.integerpart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="digits.decimalpart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="transactionResult.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <Dropdown v-model="transactionResult.tagDTO" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <div class="flex flex-row gap-2">
            <div />
            {{ slotTag.option.label }}
          </div>
        </template>
      </Dropdown>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="emitTransaction" />
    </div>
  </Dialog>
</template>

<style scoped>

</style>
