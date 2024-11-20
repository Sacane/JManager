<script setup lang="ts">
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
  previewAccountAmount: '',
  dateYear: new Date(),
  dateMonth: translate(monthFromNumber(new Date().getMonth() + 1) as string),
  tagDTO: undefined,
})

const actualSheets = ref<SheetDTO[]>([])

const { saveSheet, editSheet, findTransactionById } = useSheet()

function asDisplayableTransaction(transaction: TransactionResultDTO): any {
  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !transaction.isIncome ? `${transaction.value} €` : '',
    incomeRepresentation: transaction.isIncome ? `${transaction.value} €` : '',
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
const editTransactionInfo = reactive({
  id: 0,
  label: '',
  date: '',
  amount: 0,
  selectedMode: 'expenses',
  accountId: 0,
  integerPart: '0',
  decimalPart: '0',
  tagDTO: undefined,
  isIncome: false,
  isPreview: false,
})

function onYearChange() {
  data.year = data.dateYear.getFullYear()
  retrieveSheets()
}
function back() {
  navigateTo('/account')
}

const uDate = useDate()

// =================== REFACTO ================

const isCreationDialogVisible = ref(false)
const isEditDialogVisible = ref(false)
const digits = reactive({
  integer: '',
  decimal: '',
})
const transactionPlaceholder: TransactionCreationDTO = reactive({
  id: null,
  label: '',
  value: '0.0',
  isIncome: false,
  date: new Date(),
  tagDTO: {},
  isPreview: false,
})
function onEditPage(event: any) {
  findTransactionById(Number.parseInt(event.data.id)).then((transaction) => {
    const [integerPart, decimalPart] = transaction.value.toString().split('.')
    digits.integer = integerPart
    digits.decimal = decimalPart
    transactionPlaceholder.label = transaction.label
    transactionPlaceholder.date = uDate.dateFromString(transaction.date)
    transactionPlaceholder.value = transaction.value
    transactionPlaceholder.tagDTO = transaction.tagDTO
    transactionPlaceholder.isPreview = transaction.isPreview
    transactionPlaceholder.isIncome = transaction.isIncome
    transactionPlaceholder.id = event.data.id
    isEditDialogVisible.value = true
  }).catch(err => toastr.errorAxios(err))
}
function resetPlaceholder() {
  tag.getDefaultTag().then((tagDTO) => {
    transactionPlaceholder.tagDTO = tagDTO
  })
  digits.integer = ''
  digits.decimal = ''
  transactionPlaceholder.label = ''
  transactionPlaceholder.date = new Date()
  transactionPlaceholder.value = ''
  transactionPlaceholder.isPreview = false
  transactionPlaceholder.isIncome = false
  transactionPlaceholder.id = null
}
function cancelEditDialog() {
  isEditDialogVisible.value = false
  resetPlaceholder()
}
function cancelCreationDialog() {
  isCreationDialogVisible.value = false
}
function openCreationDialog() {
  transactionPlaceholder.isPreview = false
  isCreationDialogVisible.value = true
}
function openPreviewCreationDialog() {
  transactionPlaceholder.isPreview = true
  isCreationDialogVisible.value = true
}
function bookTransaction(transaction: TransactionCreationDTO) {
  saveSheet(data.labelAccount, transaction)
    .then((result) => {
      data.accountAmount = result.accountAmount
      data.previewAccountAmount = result.accountPreviewAmount
      const newTransaction = asDisplayableTransaction(result)
      actualSheets.value.push(newTransaction)
      isCreationDialogVisible.value = false
      resetPlaceholder()
    })
}
function editTransaction(transaction: TransactionCreationDTO) {
  editSheet(transaction, Number.parseInt(data.currentAccountId))
    .then((result: TransactionResultDTO) => {
      toastr.success('La mise a jour de la transaction s\'est correctement déroulé')
      resetPlaceholder()
      const index = actualSheets.value.findIndex(item => +item.id === +result.id)
      if (index !== -1) {
        actualSheets.value[index] = asDisplayableTransaction(result)
      }
      data.accountAmount = result.accountAmount
      data.previewAccountAmount = result.accountPreviewAmount
      isEditDialogVisible.value = false
    }).catch(err => toastr.errorAxios(err))
}
// =============================================
onMounted(() => {
  data.month = monthFromNumber(new Date().getMonth() + 1) as string
  initAccount()
  retrieveTags()
  tag.getDefaultTag().then((tagDTO) => {
    data.tagDTO = tagDTO
    editTransactionInfo.tagDTO = tagDTO
    transactionPlaceholder.tagDTO = tagDTO
  })
})
function rowStyle(row): any | undefined {
  const style = {}
  if (row.isPreview) {
    style.backgroundColor = '#a6a4a4'
  }
  if (selectedSheets.value.includes(row)) {
    if (row.isPreview) {
      style.background = '#a6a4a4'
    } else {
      style.background = '#D3D3D3'
    }
  }
  return style
}
</script>

<template>
  <ConfirmDialog />
  <div class="w-[70%] h-full flex flex-col items-center container-all self-center">
    <div class="w-full h-70% mt2px">
      <div>
        <h2 class="text-2xl font-bold info-text">
          Compte {{ data.labelAccount }}
        </h2>
        <div class="flex flex-row gap-3 justify-between">
          <Button class="h-50% min-w-30px" icon="pi pi-arrow-left" @click="back()" />
          <div class="flex flex-row gap-5 mr-5">
            <h2 class="text-2xl sold-text color-primary">
              Solde réel : {{ data.accountAmount }} €
            </h2>
            <h2 class="text-2xl sold-text preview-text">
              Solde prévisionnel : {{ data.previewAccountAmount }} €
            </h2>
          </div>
        </div>
      </div>
      <DataTable v-model:selection="selectedSheets" :header="data.labelAccount" :row-style="rowStyle" :value="actualSheets" scrollable scroll-height="flex" selection-mode="multiple" table-style="min-width: 60rem" @row-dblclick="onEditPage">
        <template #header>
          <div style="text-align: left" class="w-full">
            <div class="flex flex-row hauto justify-between">
              <Dropdown v-model="data.month" :options="uDate.months" placeholder="Selectionner un mois" class="w-full md:w-14rem" @change="retrieveSheets()" />
              <div class="w26% flex flex-row items-center">
                <div class="flex justify-center mr2">
                  <label
                    for="yearPicker"
                    class="block text-sm font-medium text-gray-700"
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
        <Column field="incomeRepresentation" header="Recettes" :header-style="{ textAlign: 'center' }" />
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
      <div class="flex flex-row gap-3 mr2 w-full justify-center">
        <Button @click="openCreationDialog">
          Ajouter une transaction
        </Button>
        <Button class="preview-button" @click="openPreviewCreationDialog">
          Ajouter une transaction prévisionnelle
        </Button>
        <Button icon="pi pi-trash" severity="danger" @click="confirmDeleteButton" />
      </div>
    </div>
  </div>
  <TransactionCreationDialog
    :visible="isCreationDialogVisible"
    title="Creer une nouvelle transaction"
    :digit-placeholder="digits"
    :transaction-placeholder="transactionPlaceholder"
    @cancel-creation="cancelCreationDialog"
    @create-transaction="bookTransaction"
  />
  <TransactionCreationDialog
    :visible="isEditDialogVisible"
    title="Mettre à jour la transaction"
    :digit-placeholder="digits"
    :transaction-placeholder="transactionPlaceholder"
    button-title="Mettre à jour"
    @cancel-creation="cancelEditDialog"
    @create-transaction="editTransaction"
  />
</template>

<style scoped lang="scss">
.container-all{
  .year-btn {
    width: auto;
    height: 5%;
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
  font-weight: 900;
  font-size: 2.5em;
  line-height: 0.9;
  font-family: aktiv, sans-serif;
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
  background-color: #a6a4a4;
  border-color: #a6a4a4;
}
.preview-button:hover {
  opacity: 0.9;
}
.sold-text {
  font-family: 'aktiv', sans-serif;
  font-weight: 900;
}
.preview-text{
  color: #a6a4a4;
}
.color-primary {
  color: var(--primary)
}
</style>
