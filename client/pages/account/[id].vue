<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import useSheet from '~/composables/useSheets'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()
const toastr = useJToast()
const selectedSheets = ref([])

const { translate, monthFromNumber, englishMonth } = useDate()
const tag = useTag()

const { findById } = useBooklet()
const { findByDate, deleteSheet, confirmPreviewTransaction } = useSheets()
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
  findByDate(englishMonth(data.month), data.year, data.labelAccount)
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
  value: 0.00,
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
  digit: 0.00,
})
const transactionPlaceholder: TransactionCreationDTO = reactive({
  id: null,
  label: '',
  value: null,
  isIncome: false,
  date: new Date(),
  tagDTO: {},
  isPreview: false,
})
function onEditPage(event: any) {
  findTransactionById(Number.parseInt(event.data.id)).then((transaction) => {
    digits.digit = transaction.value
    transactionPlaceholder.value = transaction.value
    transactionPlaceholder.label = transaction.label
    transactionPlaceholder.date = uDate.dateFromString(transaction.date)
    transactionPlaceholder.tagDTO = transaction.tagDTO
    transactionPlaceholder.isPreview = transaction.isPreview
    transactionPlaceholder.isIncome = transaction.isIncome
    transactionPlaceholder.id = event.data.id
    isEditDialogVisible.value = true
  }).catch(err => toastr.errorAxios(err))
}
function resetPlaceholder() {
  digits.digit = 0.00
  tag.getDefaultTag().then((tagDTO) => {
    transactionPlaceholder.tagDTO = tagDTO
  })
  transactionPlaceholder.label = ''
  transactionPlaceholder.date = new Date()
  transactionPlaceholder.value = 0.00
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
  resetPlaceholder()
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
      actualSheets.value = [...actualSheets.value].sort((a, b) => b.date < a.date)
      isCreationDialogVisible.value = false
      resetPlaceholder()
      toastr.success('La transaction a bien été enregistrée')
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
    data.month = translate(monthFromNumber(new Date().getMonth() + 1) as string)
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

function confirmPreview(transaction) {
  confirmPreviewTransaction(data.currentAccountId, transaction.id)
    .then((result) => {
      data.accountAmount = result.accountAmount
      data.previewAccountAmount = result.accountPreviewAmount
      const index = actualSheets.value.findIndex(v => v.id === transaction.id)
      actualSheets.value.splice(index, 1)
      actualSheets.value.push(asDisplayableTransaction(result))
      actualSheets.value = actualSheets.value.sort((a, b) => b.date < a.date)
      console.warn('actualSheets', actualSheets.value)
      toastr.success('La validation de la transaction s\'est bien déroulé !')
    })
}

function onConfirmPreview(transaction) {
  confirm.require({
    message: 'Confirmez-vous vouloir valider cette transaction prévisionnelle ?',
    header: 'Valider la transaction',
    icon: 'pi pi-check',
    acceptLabel: 'Oui',
    rejectLabel: 'Non',
    accept: () => confirmPreview(transaction),
  })
}
</script>

<template>
  <ConfirmDialog />
  <div class="container-all">
    <div class="header">
      <h2 class="font-bold info-text">
        Compte {{ data.labelAccount }}
      </h2>
    </div>
    <div class="table-container">
      <DataTable v-model:selection="selectedSheets" :header="data.labelAccount" :row-style="rowStyle" :value="actualSheets" scrollable scroll-height="flex" selection-mode="multiple" @row-dblclick="onEditPage">
        <template #header>
          <div style="text-align: left" class="w-full">
            <div class="flex flex-col h-auto gap-2px lg:(flex-row justify-between align-center)">
              <Button class="self-center h-20% lg:(h-50% min-w-30px)" icon="pi pi-arrow-left" @click="back()" />
              <Dropdown v-model="data.month" :options="uDate.months.map(u => translate(u))" placeholder="Selectionner un mois" class="md:w-14rem" @change="retrieveSheets()" />
              <div class="flex flex-col justify-between lg:(flex-row gap-5)">
                <div class="flex flex-row gap-5">
                  <h2 class="sold-text color-primary">
                    Solde réel : {{ data.accountAmount }} €
                  </h2>
                  <h2 class="sold-text preview-text">
                    Solde prévisionnel : {{ data.previewAccountAmount }} €
                  </h2>
                </div>
              </div>
              <div class="lg:w26% flex flex-row items-center">
                <div class="flex justify-center mr2">
                  <label
                    for="yearPicker"
                    class="block text-sm font-medium text-gray-700"
                  >
                    Sélectionnez une année :
                  </label>
                </div>
                <Calendar id="yearPicker" v-model="data.dateYear" class="w-full h10 text-center" view="year" date-format="yy" @date-select="onYearChange" />
              </div>
            </div>
          </div>
        </template>
        <Column selection-mode="multiple" />
        <Column sortable field="date" header="Date" :header-style="{ textAlign: 'center' }" />
        <Column field="label" header="Libellé" :header-style="{ textAlign: 'center' }" />
        <Column field="expensesRepresentation" header="Dépenses" :header-style="{ textAlign: 'center' }" />
        <Column field="incomeRepresentation" header="Recettes" :header-style="{ textAlign: 'center' }" />
        <Column field="tagDTO" header="Tag">
          <template #body="{ data }">
            <Tag :value="data.tagDTO.label" :style="getTagStyle(data.tagDTO.colorDTO)" />
          </template>
        </Column>
        <Column :style="{ width: '10rem', textAlign: 'center' }">
          <template #body="{ data }">
            <Button v-if="data.isPreview" v-tooltip="'Valider la transaction prévisionnel'" class="custom-button" rounded raised icon="pi pi-check" text aria-label="Filter" @click="onConfirmPreview(data)" />
          </template>
        </Column>
      </DataTable>
    </div>
    <div class="flex flex-col gap-5 lg:(flex-row justify-center items-center)">
      <div class="buttons-container">
        <Button @click="openCreationDialog">
          Ajouter une transaction
        </Button>
        <Button class="preview-button" @click="openPreviewCreationDialog">
          Ajouter une transaction prévisionnelle
        </Button>
      </div>
      <Button class="trash" icon="pi pi-trash" severity="danger" @click="confirmDeleteButton" />
    </div>
  </div>
  <TransactionCreationDialog
    :visible="isCreationDialogVisible"
    title="Creer une nouvelle transaction"
    :digit-placeholder="digits.digit"
    :transaction-placeholder="transactionPlaceholder"
    @cancel-creation="cancelCreationDialog"
    @create-transaction="bookTransaction"
  />
  <TransactionCreationDialog
    :visible="isEditDialogVisible"
    title="Mettre à jour la transaction"
    :digit-placeholder="digits.digit"
    :transaction-placeholder="transactionPlaceholder"
    button-title="Mettre à jour"
    @cancel-creation="cancelEditDialog"
    @create-transaction="editTransaction"
  />
</template>

<style scoped lang="scss">
.container-all {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 1rem;
  @media (max-width: 780px) {
    margin-left: 0;
    height: auto;
  }
}

.header {
  flex: 0 0 auto;
}

.table-container {
  flex: 1 1 auto;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.buttons-container {
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
  gap: 1rem;
  padding: 1rem 0;
  @media (max-width: 780px) {
    gap: 10px;
  }
}
.custom-button {
  background-color: green;
  border-color: green;
  color: white;
}

.custom-button .pi-check {
  color: white;
}
.info-text {
  text-align: center;
  color: #555;
  font-weight: 900;
  font-size: 1.6rem;
  font-family: aktiv, sans-serif;
}

.tag-container {
  height: 20px;
  align-items: center;
}

.tag-label {
  line-height: 20px;
}

.color-square {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #000;
}

.button-validate {
  border-radius: 2px;
}

.icon-validate {
  margin: 2px;
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
  font-size: 1.2rem;
}

.preview-text {
  color: #a6a4a4;
}

.color-primary {
  color: var(--primary);
}
</style>
