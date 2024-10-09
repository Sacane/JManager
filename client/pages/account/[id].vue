<script setup lang="ts">
import type { AxiosError } from 'axios'
import { useConfirm } from 'primevue/useconfirm'
import useSheet from '~/composables/useSheets'

definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()
const toastr = useJToast()
const selectedSheets = ref([])

const { translate, monthFromNumber } = useDate()
const tag = useTag()

const { findById } = useAccounts()
const { findByDate, deleteSheet } = useSheets()
const date = new Date()
const tags = ref<TagDTO[]>([])
const data = reactive({
  year: date.getFullYear(),
  month: monthFromNumber(new Date().getMonth() + 1) as string,
  labelAccount: '',
  isRangeSelected: false,
  currentSheets: [] as SheetDTO[],
  currentAccountId: '',
  accountAmount: '',
  previewAccountAmount: 0.0,
  dateYear: new Date(),
  dateMonth: translate(monthFromNumber(new Date().getMonth() + 1) as string),
  tagDTO: undefined,
})

const actualSheets = ref()

const { saveSheet, editSheet, findTransactionById } = useSheet()

function asDisplayableTransaction(transaction: SheetDTO): any {
  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !(transaction.isIncome) ? `${transaction.value} €` : '/',
    incomeRepresenttation: transaction.isIncome ? `${transaction.value} €` : '/',
    date: transaction.date,
    tagDTO: transaction.tagDTO,
  }
}
function retrieveSheets() {
  findByDate(data.month, data.year, data.labelAccount)
    .then((value: SheetAverageDTO) => {
      actualSheets.value = value.sheets.map((sheet: SheetDTO) => {
        return asDisplayableTransaction(sheet)
      })
    })
}
function retrieveTags() {
  tag.getAllTags().then(tagDTOs => tags.value = tagDTOs)
}

function initAccount() {
  findById(Number.parseFloat(route.params?.id as string))
    .then((account: AccountDTO) => {
      data.labelAccount = account.labelAccount as string
      data.currentAccountId = route.params?.id as string
      data.accountAmount = account.amount
      data.previewAccountAmount = account.previewAmount
      retrieveSheets()
    })
}

onMounted(() => {
  data.month = monthFromNumber(new Date().getMonth() + 1) as string
  initAccount()
  retrieveTags()
})

async function confirmDelete() {
  deleteSheet(Number.parseInt(data.currentAccountId), selectedSheets.value.map(sheet => sheet.id))
    .then(() => initAccount())
    .finally(() => {
      findById(Number.parseInt(data.currentAccountId)).then((account) => {
        data.accountAmount = account.amount
        data.previewAccountAmount = account.previewAmount
      })
      selectedSheets.value = []
    })
}

const confirm = useConfirm()

function confirmDeleteButton() {
  if (selectedSheets.value.length === 0) {
    return
  }
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ces éléments ?',
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    accept: () => confirmDelete(),
  })
}
const isNewTransactionDialogOpen = ref<boolean>(false)
const isEditTransactionDialogOpen = ref<boolean>(false)

const editTransactionInfo = reactive({
  id: 0,
  label: '',
  date: '',
  amount: 0,
  selectedMode: 'expenses',
  accountId: 0,
  integerPart: '0',
  decimalPart: '0',
  tagDTO: '',
  isIncome: false,
  isPreview: false,
})

function resetEditTransaction(): void {
  editTransactionInfo.label = ''
  editTransactionInfo.id = 0
  editTransactionInfo.date = new Date()
  editTransactionInfo.amount = 0
  editTransactionInfo.isIncome = false
  editTransactionInfo.tagDTO = ''
  editTransactionInfo.accountId = 0
  editTransactionInfo.selectedMode = 'expenses'
  editTransactionInfo.isPreview = false
}

function onEditPage(event: any) {
  findTransactionById(Number.parseInt(event.data.id)).then((transaction) => {
    editTransactionInfo.label = transaction.label
    editTransactionInfo.id = transaction.id
    editTransactionInfo.date = transaction.date
    editTransactionInfo.amount = transaction.value
    editTransactionInfo.isIncome = transaction.isIncome
    const [integerPart, decimalPart] = transaction.value.toString().split('.')
    editTransactionInfo.integerPart = integerPart
    editTransactionInfo.decimalPart = decimalPart
    editTransactionInfo.tagDTO = transaction.tagDTO
    editTransactionInfo.isPreview = transaction.isPreview
    isEditTransactionDialogOpen.value = true
  }).catch(err => toastr.errorAxios(err))
}

function onYearChange() {
  data.year = data.dateYear.getFullYear()
  retrieveSheets()
}
function back() {
  navigateTo('/account')
}

const uDate = useDate()

// transaction persistance

const values = reactive({
  accountId: data.currentAccountId,
  accountLabel: data.labelAccount,
  amount: 0.0,
  selectedMode: 'expenses',
  sheetLabel: '',
  date: new Date(),
  integerPart: '0',
  decimalPart: '0',
  isIncome: false,
  isPreview: false,
})

function translateChange(): void {
  editTransactionInfo.date = editTransactionInfo.date.toLocaleDateString('fr-FR').replace(/\//g, '-')
}
async function onConfirm() {
  if ((values.integerPart === '0' && values.decimalPart === '0') || values.sheetLabel === '') {
    return
  }
  const amount = `${values.integerPart}.${values.decimalPart}`
  await saveSheet(data.labelAccount, {
    id: 0,
    label: values.sheetLabel,
    value: amount,
    isIncome: values.isIncome,
    currency: '€',
    date: values.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    tagDTO: data.tagDTO,
    isPreview: values.isPreview,
  }).then(() => {
    initAccount()
  }).catch((e: AxiosError) => toastr.errorAxios(e)).finally(() => {
    isNewTransactionDialogOpen.value = false
  })
}
async function onEditTransaction() {
  if ((editTransactionInfo.integerPart === '0' && editTransactionInfo.decimalPart === '0') || editTransactionInfo.label === '') {
    return
  }
  await editSheet({
    id: editTransactionInfo.id,
    label: editTransactionInfo.label,
    value: `${editTransactionInfo.integerPart}.${editTransactionInfo.decimalPart}`,
    isIncome: (editTransactionInfo.selectedMode === 'income'),
    date: editTransactionInfo.date,
    tagDTO: editTransactionInfo.tagDTO,
  }, Number.parseInt(data.currentAccountId))
    .then((_: SheetDTO) => {
      initAccount()
      toastr.success('La mise a jour de la transaction s\'est correctement déroulé')
      isEditTransactionDialogOpen.value = false
      resetEditTransaction()
    }).catch(err => toastr.errorAxios(err))
}

function test(row): any | undefined {
  const style = {}
  if (selectedSheets.value.includes(row)) {
    style.background = '#D3D3D3'
  }
  if (row.isPreview) {
    style.border = '2px solid'
  }
  return style
}

function onOpenTransactionDialog() {
  values.isPreview = false
  isNewTransactionDialogOpen.value = true
}
function onOpenPreviewTransactionDialog() {
  values.isPreview = true
  isNewTransactionDialogOpen.value = true
}
</script>

<template>
  <ConfirmDialog />
  <div class="w-full h-full flex flex-row container-all">
    <div class="mr10px form-container p-8  bg-white mt2px">
      <div class="flex-row justify-between">
        <h2 class="text-2xl font-bold info-text">
          Les transactions sur le compte {{ data.labelAccount }}
        </h2>
        <div class="flex flex-row gap-3 justify-between">
          <Button class="w-2% h-50% min-w-30px" icon="pi pi-arrow-left" @click="back()" />
          <h2 class="text-2xl">
            Solde : {{ data.accountAmount }} €
          </h2>
          <h2 class="text-2xl">
            Solde prévisionnel : {{ data.previewAccountAmount }} €
          </h2>
        </div>
      </div>
      <DataTable v-model:selection="selectedSheets" :row-style="test" :value="actualSheets" scrollable scroll-height="500px" selection-mode="multiple" table-style="min-width: 60rem" @row-dblclick="onEditPage">
        <template #header>
          <div style="text-align: left" class="w-full">
            <div class="flex flex-row hauto justify-between">
              <Dropdown v-model="data.month" :options="uDate.months" placeholder="Selectionner un mois" class="w-full md:w-14rem" @change="retrieveSheets()" />
              <div class="w26% flex flex-row items-center">
                <div class="flex justify-center mr2">
                  <label
                    for="yearPicker"
                    class="block text-sm font-medium text-gray-700"
                    style="font-family: Arial, sans-serif;"
                  >
                    Sélectionnez une année :
                  </label>
                </div>
                <Calendar id="yearPicker" v-model="data.dateYear" class="h10 text-center" view="year" date-format="yy" @date-select="onYearChange" />
              </div>
            </div>
          </div>
        </template>
        <Column sortable field="date" header="Date" :header-style="{ textAlign: 'center' }" />
        <Column field="label" header="Libellé" :header-style="{ textAlign: 'center' }" />
        <Column field="expensesRepresentation" header="Dépenses" :header-style="{ textAlign: 'center' }" />
        <Column field="incomeRepresenttation" header="Recettes" :header-style="{ textAlign: 'center' }" />
        <Column field="tagDTO" header="Tag">
          <template #body="{ data }">
            <div class="flex flex-row align-center flex-gap-2">
              <p>
                {{ data.tagDTO.label }}
              </p>
              <div class="flex flex-col align-center justify-center">
                <div class="color-square" :style="{ backgroundColor: `rgb(${data.tagDTO.colorDTO.red}, ${data.tagDTO.colorDTO.green}, ${data.tagDTO.colorDTO.blue})` }" />
              </div>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>
    <div class="pt10px flex flex-col gap-3 mr2 ">
      <Button icon="pi pi-plus" @click="onOpenTransactionDialog()" />
      <Button class="preview-button" icon="pi pi-plus" @click="onOpenPreviewTransactionDialog()" />
      <Button icon="pi pi-trash" severity="danger" @click="confirmDeleteButton" />
    </div>
  </div>
  <Dialog v-model:visible="isNewTransactionDialogOpen" modal header="Ajouter une nouvelle transaction">
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
        <InputText id="label" v-model="values.sheetLabel" type="text" autocomplete="off" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div>
            <RadioButton v-model="values.isIncome" input-id="selection1" value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div>
            <RadioButton v-model="values.isIncome" input-id="selection2" value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="values.integerPart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="values.decimalPart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="values.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <Dropdown v-model="data.tagDTO" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <div class="flex flex-row gap-2">
            <div />
            {{ slotTag.option.label }}
          </div>
        </template>
      </Dropdown>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="onConfirm" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isNewTransactionDialogOpen = false" />
    </div>
  </Dialog>
  <Dialog v-model:visible="isEditTransactionDialogOpen" modal header="Mettre à jour la transaction">
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
        <InputText id="label" v-model="editTransactionInfo.label" type="text" autocomplete="off" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div>
            <RadioButton v-model="editTransactionInfo.selectedMode" input-id="selection1" value="expenses" />
            <label for="selection1">Dépense</label>
          </div>
          <div>
            <RadioButton v-model="editTransactionInfo.selectedMode" input-id="selection2" value="income" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="editTransactionInfo.integerPart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="editTransactionInfo.decimalPart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="editTransactionInfo.date" placeholder="Date" date-format="dd-mm-yy" @date-select="translateChange()" />
      </div>
      <Dropdown v-model="editTransactionInfo.tagDTO" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <div class="flex flex-row gap-2">
            <div />
            {{ slotTag.option.label }}
          </div>
        </template>
      </Dropdown>
      <Button label="Modifier la transaction" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="onEditTransaction" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isEditTransactionDialogOpen = false" />
    </div>
  </Dialog>
</template>

<style scoped lang="scss">
.container-all{
  .year-btn {
    width: auto;
    height: 5%;
  }
  .form-container{
    background-color: white;
    width: 100%;
    border-radius: 8px;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }
  .buttons {
    margin-top: 15px;
    .btn-small{
      padding: 6px 12px;
      margin-right: 10px;
    }
  }

}

.info-text{
  text-align: center;
  color: #555;
}

.selected-row{
  color: blue;
}

.color-square {
  width: 20px; /* Largeur du carré de couleur */
  height: 20px; /* Hauteur du carré de couleur */
  border-radius: 4px; /* Pour rendre le carré de couleur légèrement arrondi */
  border: 1px solid #000; /* Bordure du carré de couleur */
}
.test{
  background-color: blue;
}

.preview-button {
  background-color: #bc691b;
  border-color: #bc691b;
}
.preview-button:hover {
  opacity: 0.9;
}
</style>
