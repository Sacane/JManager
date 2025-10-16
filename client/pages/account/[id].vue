<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import useTransaction from '~/composables/useTransaction'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const { findByIdMonthAndYear } = useBooklet()
const route = useRoute()
const toast = useJToast()
const confirm = useConfirm()

const { englishMonth, translate, monthFromNumber, numberFromMonth } = useDate()
const tag = useTag()
const { deleteTransaction, confirmPreviewTransaction, saveTransaction, editTransaction, findTransactionById } = useTransaction()

const selectedSheets = ref<TransactionCreationDTO[]>([])
const actualSheets = ref<TransactionCreationDTO[]>([])
const tags = ref<TagDTO[]>([])

const isCreationDialogVisible = ref(false)
const isEditDialogVisible = ref(false)

const bookletData = reactive({
  id: '',
  label: '',
  realSold: 0.00,
  previewSold: 0.00,
  year: new Date().getFullYear(),
  month: monthFromNumber(new Date().getMonth() + 1) as string,
  dateYear: new Date(),
})

const currentTransaction = reactive<TransactionCreationDTO>({
  id: null,
  label: '',
  value: null,
  isIncome: false,
  date: new Date().toString(),
  tagDTO: {},
  isPreview: false,
})

const displayMonth = computed(() => translate(bookletData.month))
const transactionsCount = computed(() => actualSheets.value.length)
const previewTransactionsCount = computed(() => actualSheets.value.filter(t => t.isPreview).length)
const hasSelection = computed(() => selectedSheets.value.length > 0)

function asDisplayableTransaction(transaction: TransactionCreationDTO): any {
  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    incomeRepresentation: transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    date: transaction.date.toString(),
    tagDTO: transaction.tagDTO,
  }
}

function resetTransaction() {
  currentTransaction.id = null
  currentTransaction.label = ''
  currentTransaction.value = null
  currentTransaction.date = new Date()
  currentTransaction.isPreview = false
  currentTransaction.isIncome = false
  tag.getDefaultTag().then((tagDTO) => {
    currentTransaction.tagDTO = tagDTO
  })
}

async function loadBookletData() {
  try {
    const accountId = Number.parseInt(route.params?.id as string)
    const month = numberFromMonth(bookletData.month) as number

    const result: BookletReport = await findByIdMonthAndYear(accountId, month, bookletData.year)

    bookletData.label = result.label
    bookletData.id = route.params?.id as string
    bookletData.realSold = Number.parseFloat(result.realSold)
    bookletData.previewSold = Number.parseFloat(result.previewSold)

    actualSheets.value = result.transactions
      .map(asDisplayableTransaction)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
  } catch (err) {
    toast.errorAxios(err)
  }
}

async function retrieveTags() {
  try {
    tags.value = await tag.getAllTags()
  } catch (err) {
    toast.errorAxios(err)
  }
}

function onMonthChange(event: any) {
  bookletData.month = englishMonth(event.value)
  loadBookletData()
}

function onYearChange() {
  bookletData.year = bookletData.dateYear.getFullYear()
  loadBookletData()
}

function openCreationDialog() {
  resetTransaction()
  currentTransaction.isPreview = false
  isCreationDialogVisible.value = true
}

function openPreviewCreationDialog() {
  resetTransaction()
  currentTransaction.isPreview = true
  isCreationDialogVisible.value = true
}

async function bookTransaction(transaction: TransactionCreationDTO) {
  try {
    const result = await saveTransaction(bookletData.label, transaction)

    bookletData.realSold = Number.parseFloat(result.accountAmount)
    bookletData.previewSold = Number.parseFloat(result.accountPreviewAmount)

    const newTransaction = asDisplayableTransaction(result)
    actualSheets.value.push(newTransaction)
    actualSheets.value.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

    isCreationDialogVisible.value = false
    resetTransaction()
    toast.success('Transaction enregistrée avec succès')
  } catch (err) {
    toast.errorAxios(err)
  }
}

async function onEditTransaction(event: any) {
  try {
    const transaction = await findTransactionById(Number.parseInt(event.data.id))

    currentTransaction.id = event.data.id
    currentTransaction.label = transaction.label
    currentTransaction.value = transaction.value
    currentTransaction.date = transaction.date
    currentTransaction.tagDTO = transaction.tagDTO
    currentTransaction.isPreview = transaction.isPreview
    currentTransaction.isIncome = transaction.isIncome

    isEditDialogVisible.value = true
  } catch (err) {
    toast.errorAxios(err)
  }
}

async function applyEditTransaction(transaction: TransactionCreationDTO) {
  try {
    const result: TransactionResultDTO = await editTransaction(transaction, Number.parseInt(bookletData.id))

    const index = actualSheets.value.findIndex(item => (+item?.id!) === +result.id)
    if (index !== -1) {
      actualSheets.value[index] = asDisplayableTransaction(result)
    }

    bookletData.realSold = Number.parseFloat(result.accountAmount)
    bookletData.previewSold = Number.parseFloat(result.accountPreviewAmount)

    isEditDialogVisible.value = false
    resetTransaction()
    toast.success('Transaction mise à jour avec succès')
  } catch (err) {
    toast.errorAxios(err)
  }
}

async function confirmDelete() {
  try {
    await deleteTransaction(
      Number.parseInt(bookletData.id),
      selectedSheets.value.map(sheet => Number.parseInt(sheet.id as string)),
    )

    selectedSheets.value.forEach((sheet) => {
      const index = actualSheets.value.findIndex(item => (+item?.id!) === +sheet!.id)
      if (index !== -1) {
        actualSheets.value.splice(index, 1)
        const value = Number.parseFloat(sheet?.value?.toString() ?? '0')

        if (sheet.isPreview) {
          bookletData.previewSold = sheet.isIncome
            ? bookletData.previewSold - value
            : bookletData.previewSold + value
        } else {
          bookletData.realSold = sheet.isIncome
            ? bookletData.realSold - value
            : bookletData.realSold + value
        }
      }
    })

    selectedSheets.value = []
    toast.success('Transactions supprimées avec succès')
  } catch (err) {
    toast.errorAxios(err)
  }
}

function confirmDeleteButton() {
  if (!hasSelection.value) return

  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer ${selectedSheets.value.length} transaction${selectedSheets.value.length > 1 ? 's' : ''} ?`,
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptClass: 'p-button-danger',
    accept: () => confirmDelete(),
  })
}

async function confirmPreview(transaction: TransactionCreationDTO) {
  try {
    const result = await confirmPreviewTransaction(bookletData.id, transaction.id as string)

    bookletData.realSold = Number.parseFloat(result.accountAmount)
    bookletData.previewSold = Number.parseFloat(result.accountPreviewAmount)

    const index = actualSheets.value.findIndex(v => v.id === transaction.id)
    if (index !== -1) {
      actualSheets.value.splice(index, 1)
      actualSheets.value.push(asDisplayableTransaction(result))
      actualSheets.value.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    }

    toast.success('Transaction validée avec succès')
  } catch (err) {
    toast.errorAxios(err)
  }
}

function onConfirmPreview(transaction: TransactionCreationDTO) {
  confirm.require({
    message: 'Voulez-vous valider cette transaction prévisionnelle ?',
    header: 'Validation de transaction',
    icon: 'pi pi-check',
    acceptLabel: 'Valider',
    rejectLabel: 'Annuler',
    accept: () => confirmPreview(transaction),
  })
}

function rowClass(row: TransactionCreationDTO): string {
  if (row.isPreview) return 'preview-row'
  return ''
}

onMounted(async () => {
  bookletData.month = monthFromNumber(new Date().getMonth() + 1) as string
  await loadBookletData()
  await retrieveTags()

  const defaultTag = await tag.getDefaultTag()
  currentTransaction.tagDTO = defaultTag
})
</script>

<template>
  <ConfirmDialog />

  <div class="booklet-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-top">
        <Button
          class="back-button"
          icon="pi pi-arrow-left"
          text
          rounded
          @click="navigateTo('/account')"
        />

        <div class="booklet-title">
          <h1>{{ bookletData.label }}</h1>
          <div class="booklet-stats">
            <span class="stat-badge">
              <i class="pi pi-list" />
              {{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}
            </span>
            <span v-if="previewTransactionsCount > 0" class="stat-badge preview">
              <i class="pi pi-clock" />
              {{ previewTransactionsCount }} en attente
            </span>
          </div>
        </div>
      </div>

      <!-- Filtres et soldes -->
      <div class="header-controls">
        <div class="date-filters">
          <Dropdown
            v-model="displayMonth"
            :options="useDate().months.map(u => translate(u))"
            placeholder="Mois"
            class="month-dropdown"
            @change="onMonthChange($event)"
          />
          <Calendar
            v-model="bookletData.dateYear"
            view="year"
            date-format="yy"
            class="year-picker"
            @date-select="onYearChange"
          />
        </div>

        <div class="balance-cards">
          <div class="balance-card real">
            <div class="balance-label">
              <i class="pi pi-wallet" />
              <span>Solde réel</span>
            </div>
            <div class="balance-amount">
              {{ bookletData.realSold.toFixed(2) }} €
            </div>
          </div>

          <div class="balance-card preview">
            <div class="balance-label">
              <i class="pi pi-chart-line" />
              <span>Solde prévisionnel</span>
            </div>
            <div class="balance-amount">
              {{ bookletData.previewSold.toFixed(2) }} €
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions principales -->
    <div class="main-actions">
      <div class="action-buttons">
        <Button
          class="btn-primary"
          icon="pi pi-plus"
          label="Nouvelle transaction"
          @click="openCreationDialog"
        />
        <Button
          class="btn-secondary"
          icon="pi pi-clock"
          label="Transaction prévisionnelle"
          @click="openPreviewCreationDialog"
        />
      </div>

      <Button
        v-if="hasSelection"
        class="delete-button"
        icon="pi pi-trash"
        :label="`Supprimer (${selectedSheets.length})`"
        severity="danger"
        @click="confirmDeleteButton"
      />
    </div>

    <!-- Table des transactions -->
    <div class="transactions-table">
      <DataTable
        v-model:selection="selectedSheets"
        :value="actualSheets"
        :row-class="rowClass"
        scrollable
        scroll-height="flex"
        selection-mode="multiple"
        :meta-key-selection="false"
        striped-rows
        @row-dblclick="onEditTransaction"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-inbox" />
            <h3>Aucune transaction</h3>
            <p>Commencez par créer votre première transaction</p>
            <Button
              class="btn-primary"
              icon="pi pi-plus"
              label="Créer une transaction"
              @click="openCreationDialog"
            />
          </div>
        </template>

        <Column selection-mode="multiple" :style="{ width: '3rem' }" />

        <Column field="date" header="Date" sortable :style="{ minWidth: '120px' }">
          <template #body="{ data }">
            <div class="date-cell">
              <i class="pi pi-calendar" />
              <span>{{ data.date }}</span>
            </div>
          </template>
        </Column>

        <Column field="label" header="Libellé" :style="{ minWidth: '200px' }">
          <template #body="{ data }">
            <div class="label-cell">
              <span class="transaction-label">{{ data.label }}</span>
              <i v-if="data.isPreview" v-tooltip="'Transaction prévisionnelle'" class="pi pi-clock preview-icon" />
            </div>
          </template>
        </Column>

        <Column field="expensesRepresentation" header="Dépenses" :style="{ minWidth: '120px' }">
          <template #body="{ data }">
            <span v-if="!data.isIncome" class="amount expense">
              {{ data.expensesRepresentation }}
            </span>
            <span v-else class="amount-placeholder">-</span>
          </template>
        </Column>

        <Column field="incomeRepresentation" header="Recettes" :style="{ minWidth: '120px' }">
          <template #body="{ data }">
            <span v-if="data.isIncome" class="amount income">
              {{ data.incomeRepresentation }}
            </span>
            <span v-else class="amount-placeholder">-</span>
          </template>
        </Column>

        <Column field="tagDTO" header="Catégorie" :style="{ minWidth: '150px' }">
          <template #body="{ data }">
            <Tag :value="data.tagDTO.label" :style="getTagStyle(data.tagDTO.colorDTO)" />
          </template>
        </Column>

        <Column :style="{ width: '100px', textAlign: 'center' }">
          <template #body="{ data }">
            <Button
              v-if="data.isPreview"
              v-tooltip="'Valider la transaction'"
              class="validate-button"
              icon="pi pi-check"
              text
              rounded
              severity="success"
              @click="onConfirmPreview(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>

  <!-- Dialogs -->
  <TransactionCreationDialog
    :visible="isCreationDialogVisible"
    title="Nouvelle transaction"
    :digit-placeholder="currentTransaction.value as number"
    :transaction-placeholder="currentTransaction"
    @cancel-creation="isCreationDialogVisible = false"
    @create-transaction="bookTransaction"
  />

  <TransactionCreationDialog
    :visible="isEditDialogVisible"
    title="Modifier la transaction"
    :digit-placeholder="currentTransaction.value as number"
    :transaction-placeholder="currentTransaction"
    button-title="Mettre à jour"
    @cancel-creation="isEditDialogVisible = false"
    @create-transaction="applyEditTransaction"
  />
</template>

<style scoped lang="scss">
.booklet-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 20px;
  gap: 20px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);

  @media (max-width: 768px) {
    padding: 10px;
    gap: 15px;
  }
}

// ===== HEADER =====
.page-header {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(130, 42, 204, 0.08);
  border: 1px solid rgba(130, 42, 204, 0.1);

  @media (max-width: 768px) {
    padding: 16px;
  }
}

.header-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-button {
  color: #822acc;

  &:hover {
    background: rgba(130, 42, 204, 0.1);
  }
}

.booklet-title {
  flex: 1;

  h1 {
    font-size: 1.75rem;
    font-weight: 800;
    background: linear-gradient(135deg, #822acc, #651e9e);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 8px 0;

    @media (max-width: 768px) {
      font-size: 1.5rem;
    }
  }
}

.booklet-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #822acc10, #651e9e10);
  border: 1px solid rgba(130, 42, 204, 0.2);
  border-radius: 50px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #822acc;

  &.preview {
    background: linear-gradient(135deg, #f59e0b10, #d9770610);
    border-color: rgba(245, 158, 11, 0.3);
    color: #d97706;
  }

  i {
    font-size: 0.75rem;
  }
}

.header-controls {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 24px;
  align-items: center;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.date-filters {
  display: flex;
  gap: 12px;

  @media (max-width: 768px) {
    flex-direction: column;
  }

  :deep(.p-dropdown),
  :deep(.p-calendar) {
    border: 2px solid #e5e7eb;
    border-radius: 12px;

    &:hover {
      border-color: #822acc;
    }

    &:focus-within {
      border-color: #822acc;
      box-shadow: 0 0 0 3px rgba(130, 42, 204, 0.1);
    }
  }
}

.month-dropdown,
.year-picker {
  min-width: 150px;

  @media (max-width: 768px) {
    width: 100%;
  }
}

.balance-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.balance-card {
  padding: 20px;
  border-radius: 16px;
  border: 2px solid;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
  }

  &.real {
    background: linear-gradient(135deg, rgba(130, 42, 204, 0.05), rgba(101, 30, 158, 0.05));
    border-color: #822acc;

    &::before {
      background: linear-gradient(90deg, #822acc, #651e9e);
    }

    .balance-label {
      color: #822acc;

      i {
        color: #822acc;
      }
    }

    .balance-amount {
      color: #822acc;
    }

    &:hover {
      background: linear-gradient(135deg, rgba(130, 42, 204, 0.08), rgba(101, 30, 158, 0.08));
      box-shadow: 0 4px 12px rgba(130, 42, 204, 0.15);
    }
  }

  &.preview {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.05), rgba(217, 119, 6, 0.05));
    border-color: #f59e0b;

    &::before {
      background: linear-gradient(90deg, #f59e0b, #d97706);
    }

    .balance-label {
      color: #d97706;

      i {
        color: #f59e0b;
      }
    }

    .balance-amount {
      color: #d97706;
    }

    &:hover {
      background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(217, 119, 6, 0.08));
      box-shadow: 0 4px 12px rgba(245, 158, 11, 0.15);
    }
  }
}

.balance-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  margin-bottom: 8px;

  i {
    font-size: 1rem;
  }
}

.balance-amount {
  font-size: 1.75rem;
  font-weight: 800;

  @media (max-width: 768px) {
    font-size: 1.5rem;
  }
}

// ===== ACTIONS =====
.main-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  @media (max-width: 768px) {
    width: 100%;

    button {
      flex: 1;
    }
  }
}

.btn-primary {
  background: linear-gradient(135deg, #822acc, #651e9e);
  border: none;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.25);
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, #6a22aa, #541a82);
    box-shadow: 0 6px 16px rgba(130, 42, 204, 0.35);
    transform: translateY(-2px);
  }
}

.btn-secondary {
  background: white;
  color: #d97706;
  border: 2px solid #f59e0b;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.1));
    border-color: #d97706;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
    transform: translateY(-2px);
  }
}

.delete-button {
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.25);
    transform: translateY(-2px);
  }

  @media (max-width: 768px) {
    width: 100%;
  }
}

// ===== TABLE =====
.transactions-table {
  flex: 1;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(130, 42, 204, 0.08);
  border: 1px solid rgba(130, 42, 204, 0.1);

  :deep(.p-datatable) {
    .p-datatable-thead > tr > th {
      background: var(--primary);
      color: white;
      font-weight: 700;
      padding: 16px;
      border: none;
      text-transform: uppercase;
      font-size: 0.875rem;
      letter-spacing: 0.05em;
    }

    .p-datatable-tbody > tr {
      transition: all 0.2s ease;
      border-bottom: 1px solid #f3f4f6;
      background: white;

      &:hover {
        background: linear-gradient(135deg, #faf5ff, #f9f5ff);
      }

      &.p-highlight {
        background: linear-gradient(135deg, #f3e8ff, #ede9fe);
        border-left: 3px solid #822acc;
      }

      &.preview-row {
        background: linear-gradient(135deg, #fffbeb, #fef3c7);
        border-left: 4px solid #f59e0b;

        &:hover {
          background: linear-gradient(135deg, #fef9c3, #fef08a);
        }

        &.p-highlight {
          background: linear-gradient(135deg, #fef3c7, #fde68a);
          border-left: 4px solid #d97706;
        }
      }

      > td {
        padding: 16px;
        border: none;
      }
    }

    .p-checkbox {
      .p-checkbox-box {
        border-color: #822acc;

        &.p-highlight {
          background: linear-gradient(135deg, #822acc, #651e9e);
          border-color: #822acc;
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  i {
    font-size: 4rem;
    color: #d1d5db;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 1.25rem;
    font-weight: 700;
    color: #374151;
    margin: 0 0 8px 0;
  }

  p {
    font-size: 1rem;
    color: #6b7280;
    margin: 0 0 24px 0;
  }
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-weight: 500;

  i {
    font-size: 0.875rem;
    color: #822acc;
  }
}

.label-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.transaction-label {
  font-weight: 600;
  color: #1f2937;
}

.preview-icon {
  color: #f59e0b;
  font-size: 0.875rem;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.amount {
  font-weight: 800;
  font-size: 1.1rem;
  font-family: 'Courier New', monospace;

  &.expense {
    color: #ef4444;

    &::before {
      content: '- ';
    }
  }

  &.income {
    color: #10b981;

    &::before {
      content: '+ ';
    }
  }
}

.amount-placeholder {
  color: #e5e7eb;
  font-weight: 600;
}

.validate-button {
  color: #10b981;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(16, 185, 129, 0.15);
    transform: scale(1.1);
  }
}

:deep(.p-tag) {
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>
