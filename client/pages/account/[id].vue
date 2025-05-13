<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import useTransaction from '~/composables/useTransaction'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()
const toastr = useJToast()
const selectedSheets = ref<TransactionCreationDTO[]>([])

const { translate, monthFromNumber, englishMonth } = useDate()
const tag = useTag()

const { findById } = useBooklet()
const { findByDate, deleteTransaction, confirmPreviewTransaction } = useTransaction()
const date = new Date()
const tags = ref<TagDTO[]>([])

const formData = reactive<{
  year: number
  month: string
  labelAccount: string
  isRangeSelected: boolean
  currentSheets: TransactionCreationDTO[]
  currentAccountId: string
  accountAmount: number
  previewAccountAmount: number
  dateYear: Date
  dateMonth: string
  tagDTO: TagDTO | null
}>({
  year: date.getFullYear(),
  month: monthFromNumber(new Date().getMonth() + 1) as string,
  labelAccount: '',
  isRangeSelected: false,
  currentSheets: [] as TransactionCreationDTO[],
  currentAccountId: '',
  accountAmount: 0.00,
  previewAccountAmount: 0.00,
  dateYear: new Date(),
  dateMonth: translate(monthFromNumber(new Date().getMonth() + 1) as string),
  tagDTO: null,
})

const actualSheets = ref<TransactionCreationDTO[]>([])

const { saveTransaction, editTransaction, findTransactionById } = useTransaction()

function asDisplayableTransaction(transaction: TransactionCreationDTO): any {
  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '',
    incomeRepresentation: transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '',
    date: transaction.date,
    tagDTO: transaction.tagDTO,
  }
}
function retrieveSheets() {
  findByDate(englishMonth(formData.month), formData.year, formData.labelAccount)
    .then((value: SheetAverageDTO) => {
      actualSheets.value = value.transactions.map((sheet: TransactionCreationDTO) => {
        return asDisplayableTransaction(sheet)
      })
    })
}
function retrieveTags() {
  tag.getAllTags().then(tagDTOs => tags.value = tagDTOs)
}

function initAccount() {
  findById(Number.parseFloat(route.params?.id as string))
    .then((account: BookletDTO) => {
      formData.labelAccount = account.labelAccount as string
      formData.currentAccountId = route.params?.id as string
      formData.accountAmount = account.amount
      formData.previewAccountAmount = account.previewAmount
      retrieveSheets()
    })
}

async function confirmDelete() {
  deleteTransaction(Number.parseInt(formData.currentAccountId), selectedSheets.value.map(sheet => sheet.id))
    .then(() => initAccount())
    .finally(() => {
      findById(Number.parseInt(formData.currentAccountId)).then((account) => {
        formData.accountAmount = account.amount
        formData.previewAccountAmount = account.previewAmount
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

function onYearChange() {
  formData.year = formData.dateYear.getFullYear()
  retrieveSheets()
}
function back() {
  navigateTo('/account')
}

const uDate = useDate()

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
  transactionPlaceholder.value = null
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
  saveTransaction(formData.labelAccount, transaction)
    .then((result) => {
      formData.accountAmount = result.accountAmount
      formData.previewAccountAmount = result.accountPreviewAmount
      const newTransaction = asDisplayableTransaction(result)
      actualSheets.value.push(newTransaction)
      actualSheets.value = [...actualSheets.value].sort((a, b) => new Date(b.date) - new Date(a.date))
      isCreationDialogVisible.value = false
      resetPlaceholder()
      toastr.success('La transaction a bien été enregistrée')
    })
}
function applyEditTransaction(transaction: TransactionCreationDTO) {
  editTransaction(transaction, Number.parseInt(formData.currentAccountId))
    .then((result: TransactionResultDTO) => {
      toastr.success('La mise a jour de la transaction s\'est correctement déroulé')
      resetPlaceholder()
      const index = actualSheets.value.findIndex(item => (((item?.id) ? (+item?.id) : 0) === +result.id))
      if (index !== -1) {
        actualSheets.value[index] = asDisplayableTransaction(result)
      }
      formData.accountAmount = result.accountAmount
      formData.previewAccountAmount = result.accountPreviewAmount
      isEditDialogVisible.value = false
    }).catch(err => toastr.errorAxios(err))
}

onMounted(() => {
  formData.month = monthFromNumber(new Date().getMonth() + 1) as string
  initAccount()
  retrieveTags()
  tag.getDefaultTag().then((tagDTO: TagDTO) => {
    formData.tagDTO = tagDTO
    transactionPlaceholder.tagDTO = tagDTO
    formData.month = translate(monthFromNumber(new Date().getMonth() + 1) as string)
  })
})
function rowStyle(row: TransactionCreationDTO): any | undefined {
  const style: {
    backgroundColor?: string
    background?: string
    color?: string
  } = {}
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

function confirmPreview(transaction: TransactionCreationDTO) {
  confirmPreviewTransaction(formData.currentAccountId, transaction.id as string)
    .then((result) => {
      formData.accountAmount = result.accountAmount
      formData.previewAccountAmount = result.accountPreviewAmount
      const index = actualSheets.value.findIndex(v => v.id === transaction.id)
      actualSheets.value.splice(index, 1)
      actualSheets.value.push(asDisplayableTransaction(result))
      actualSheets.value = actualSheets.value.sort((a, b) => new Date(b.date).getDate() - new Date(a.date).getDate())
      toastr.success('La validation de la transaction s\'est bien déroulé !')
    })
}

function onConfirmPreview(transaction: TransactionCreationDTO) {
  confirm.require({
    message: 'Confirmez-vous vouloir valider cette transaction attendue ?',
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
    <div class="account-label">
      <span>Livret {{ formData.labelAccount }}</span>
    </div>
    <div class="table-container">
      <DataTable v-model:selection="selectedSheets" :header="formData.labelAccount" :row-style="rowStyle" :value="actualSheets" scrollable scroll-height="flex" selection-mode="multiple" @row-dblclick="onEditPage">
        <template #header>
          <div style="text-align: left; position: sticky; top: 0" class="w-full">
            <div class="flex flex-col h-auto gap-2px lg:(flex-row justify-between align-center)">
              <Button class="btn-primary self-center h-20% lg:(h-50% min-w-30px)" icon="pi pi-arrow-left" @click="back()" />
              <div class="lg:w26% flex flex-row items-center">
                <Dropdown v-model="formData.month" :options="uDate.months.map(u => translate(u))" placeholder="Selectionner un mois" class="md:w-14rem" @change="retrieveSheets()" />
                <Calendar id="yearPicker" v-model="formData.dateYear" class="md:w-14rem" view="year" date-format="yy" @date-select="onYearChange" />
              </div>
              <div class="flex flex-col justify-between lg:(flex-row gap-3)">
                <BalanceCard title="Solde réel" :amount="formData.accountAmount.toString()" is-preview />
                <BalanceCard title="Solde prévisionnel" :amount="formData.previewAccountAmount.toString()" is-preview />
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
            <Button v-if="data.isPreview" v-tooltip="'Valider la transaction attendue'" class="btn-primary color-white" rounded raised icon="pi pi-check" text aria-label="Filter" @click="onConfirmPreview(data)" />
          </template>
        </Column>
      </DataTable>
    </div>
    <div class="flex flex-col gap-5 lg:(flex-row justify-center items-center)">
      <div class="buttons-container">
        <Button class="btn-primary" @click="openCreationDialog">
          Ajouter une transaction
        </Button>
        <Button class="preview-button" @click="openPreviewCreationDialog">
          Ajouter une transaction attendue
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
    @create-transaction="applyEditTransaction"
  />
</template>

<style scoped lang="scss">
.container-all {
  display: flex;
  flex-direction: column;
  height: 100%;
  margin-left: 15px;
  max-width: 1950px;
  @media (max-width: 780px) {
    margin-left: 0;
    height: auto;
  }
}

.table-container :deep(.p-datatable-thead) {
  position: sticky;
  top: 0;
}

.account-label {
  width: 300px;
  background-color: #f0f0f0;
  padding: 10px 15px;
  text-align: center;
  font-size: 1.5rem;
  font-weight: 900;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: inline-block;
  color: #555;
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
