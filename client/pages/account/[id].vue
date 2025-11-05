<script setup lang="ts">
import type { AxiosError } from 'axios'
import { useConfirm } from 'primevue/useconfirm'
import useCsvImport from '~/composables/useCsvImport'
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
const { downloadCsvExport } = useCsvImport()

const selectedSheets = ref<TransactionCreationDTO[]>([])
const actualSheets = ref<TransactionCreationDTO[]>([])
const tags = ref<TagDTO[]>([])

const isCreationDialogVisible = ref(false)
const isEditDialogVisible = ref(false)
const isMobile = ref(false)
const csvImportDialogRef = ref<any>(null)

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
  date: new Date(),
  tagDTO: {
    tagId: undefined,
    label: '',
    colorDTO: { red: 0, green: 0, blue: 0 },
    isDefault: false,
  },
  isPreview: false,
})

const displayMonth = computed(() => translate(bookletData.month))
const transactionsCount = computed(() => actualSheets.value.length)
const previewTransactionsCount = computed(() => actualSheets.value.filter(t => t.isPreview).length)
const hasSelection = computed(() => selectedSheets.value.length > 0)

function asDisplayableTransaction(transaction: TransactionResultDTO): any {
  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    incomeRepresentation: transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    date: transaction.date,
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
    const accountId = (route.params as any)?.id as string
    const month = numberFromMonth(bookletData.month) as number

    const result: BookletReport = await findByIdMonthAndYear(accountId, month, bookletData.year)

    bookletData.label = result.label
    bookletData.id = (route.params as any)?.id as string
    bookletData.realSold = Number.parseFloat(result.realSold)
    bookletData.previewSold = Number.parseFloat(result.previewSold)

    actualSheets.value = result.transactions
      .map(asDisplayableTransaction)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
  } catch (err) {
    toast.errorAxios(err as AxiosError)
    console.error(err)
  }
}

async function retrieveTags() {
  try {
    tags.value = await tag.getAllTags()
  } catch (err) {
    toast.errorAxios(err as AxiosError)
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
    const transaction = await findTransactionById(event.data.id)

    currentTransaction.id = event.data.id
    currentTransaction.label = transaction.label
    currentTransaction.value = transaction.value
    currentTransaction.date = new Date(transaction.date)
    currentTransaction.tagDTO = transaction.tagDTO
    currentTransaction.isPreview = transaction.isPreview
    currentTransaction.isIncome = transaction.isIncome

    isEditDialogVisible.value = true
  } catch (err) {
    toast.errorAxios(err as AxiosError)
  }
}

async function applyEditTransaction(transaction: TransactionCreationDTO) {
  try {
    const result: TransactionResultDTO = await editTransaction(transaction, bookletData.id)

    const index = actualSheets.value.findIndex(item => item?.id === result.id)
    if (index !== -1) {
      actualSheets.value[index] = asDisplayableTransaction(result)
    }

    bookletData.realSold = Number.parseFloat(result.accountAmount)
    bookletData.previewSold = Number.parseFloat(result.accountPreviewAmount)

    isEditDialogVisible.value = false
    resetTransaction()
    toast.success('Transaction mise à jour avec succès')
  } catch (err) {
    toast.errorAxios(err as AxiosError)
  }
}

async function confirmDelete() {
  try {
    await deleteTransaction(
      bookletData.id,
      selectedSheets.value.map(sheet => sheet.id as string),
    )

    selectedSheets.value.forEach((sheet) => {
      const index = actualSheets.value.findIndex(item => item?.id === sheet!.id)
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
    toast.errorAxios(err as AxiosError)
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
    toast.errorAxios(err as AxiosError)
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

function toggleSelection(transaction: TransactionCreationDTO) {
  const index = selectedSheets.value.findIndex(t => t.id === transaction.id)
  if (index === -1) {
    selectedSheets.value.push(transaction)
  } else {
    selectedSheets.value.splice(index, 1)
  }
}

function isSelected(transaction: TransactionCreationDTO): boolean {
  return selectedSheets.value.some(t => t.id === transaction.id)
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

function openCsvImportDialog() {
  csvImportDialogRef.value?.openDialog()
}

function openCsvExportDialog() {
  // Filtrer uniquement les transactions non prévisionnelles
  const nonPreviewTransactions = actualSheets.value.filter(t => !t.isPreview)

  if (nonPreviewTransactions.length === 0) {
    toast.warn('Aucune transaction à exporter (les transactions prévisionnelles ne sont pas exportées)')
    return
  }

  confirm.require({
    message: `Voulez-vous télécharger le fichier CSV contenant ${nonPreviewTransactions.length} transaction${nonPreviewTransactions.length > 1 ? 's' : ''} non prévisionnelle${nonPreviewTransactions.length > 1 ? 's' : ''} pour ${displayMonth.value} ${bookletData.year} ?`,
    header: 'Exporter au format CSV',
    icon: 'pi pi-file-export',
    acceptLabel: 'Télécharger',
    rejectLabel: 'Annuler',
    accept: async () => {
      try {
        const transactionIds = nonPreviewTransactions
          .map(t => t.id)
          .filter(id => id != null) as string[]

        const filename = `transactions_${bookletData.label.replace(/\s+/g, '_')}_${bookletData.month}_${bookletData.year}.csv`

        await downloadCsvExport(transactionIds, filename)
        toast.success('Fichier CSV téléchargé avec succès !')
      } catch (err) {
        console.error('Erreur lors de l\'export CSV:', err)
        toast.errorAxios(err as AxiosError)
      }
    },
  })
}

function onCsvImportSuccess(result: CsvImportResultDTO) {
  // Recharger les données du booklet après l'importation
  loadBookletData()
  toast.success(`${result.successCount} transactions importées avec succès !`)
}

onMounted(async () => {
  bookletData.month = monthFromNumber(new Date().getMonth() + 1) as string
  await loadBookletData()
  await retrieveTags()

  currentTransaction.tagDTO = await tag.getDefaultTag()

  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <ConfirmDialog />

  <div class="booklet-page">
    <!-- Header Compact -->
    <div class="page-header">
      <div class="header-main">
        <div class="header-left">
          <Button
            class="back-button"
            icon="pi pi-arrow-left"
            text
            rounded
            @click="navigateTo('/account')"
          />
          <div class="booklet-info">
            <h1>{{ bookletData.label }}</h1>
            <div class="meta-info">
              <span class="meta-item">
                {{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}
              </span>
              <span v-if="previewTransactionsCount > 0" class="meta-item preview">
                {{ previewTransactionsCount }} en attente
              </span>
            </div>
          </div>
        </div>

        <div class="header-right">
          <div class="balance-compact">
            <div class="balance-item">
              <span class="balance-label-compact">Réel</span>
              <span class="balance-value-compact real">{{ bookletData.realSold.toFixed(2) }} €</span>
            </div>
            <div class="balance-divider" />
            <div class="balance-item">
              <span class="balance-label-compact">Prévisionnel</span>
              <span class="balance-value-compact preview">{{ bookletData.previewSold.toFixed(2) }} €</span>
            </div>
          </div>

          <div class="date-filters-compact">
            <Select
              v-model="displayMonth"
              :options="useDate().months.map(u => translate(u))"
              placeholder="Mois"
              class="month-dropdown-compact"
              @change="onMonthChange($event)"
            />
            <DatePicker
              v-model="bookletData.dateYear"
              view="year"
              date-format="yy"
              class="year-picker-compact"
              placeholder="Année"
              :show-icon="true"
              icon-display="input"
              @date-select="onYearChange"
            />
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
          :label="isMobile ? 'Transaction' : 'Nouvelle transaction'"
          @click="openCreationDialog"
        />
        <Button
          class="btn-secondary"
          icon="pi pi-clock"
          :label="isMobile ? 'Prévisionnelle' : 'Transaction prévisionnelle'"
          @click="openPreviewCreationDialog"
        />
        <Button
          class="btn-csv-import"
          icon="pi pi-file-import"
          :label="isMobile ? 'CSV' : 'Importer CSV'"
          @click="openCsvImportDialog"
        />
        <Button
          class="btn-csv-export"
          icon="pi pi-file-export"
          :label="isMobile ? 'Export' : 'Exporter CSV'"
          @click="openCsvExportDialog"
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

    <!-- Table des transactions (Desktop) -->
    <div v-if="!isMobile" class="transactions-table">
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

        <Column field="date" header="Date" :sortable="true" :style="{ minWidth: '120px' }">
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

    <!-- Liste des transactions (Mobile) -->
    <div v-else class="transactions-mobile">
      <div v-if="actualSheets.length === 0" class="empty-state-mobile">
        <i class="pi pi-inbox" />
        <h3>Aucune transaction</h3>
        <p>Commencez par créer votre première transaction</p>
        <Button
          class="btn-primary"
          icon="pi pi-plus"
          label="Créer"
          @click="openCreationDialog"
        />
      </div>

      <div v-else class="transaction-cards">
        <div
          v-for="(transaction, tIndex) in actualSheets"
          :key="transaction.id || `t-${tIndex}`"
          class="transaction-card" :class="[{
            'preview-card': transaction.isPreview,
            'selected-card': isSelected(transaction),
          }]"
          @click="toggleSelection(transaction)"
        >
          <!-- Header de la carte -->
          <div class="card-header">
            <div class="card-header-left">
              <Checkbox
                :model-value="isSelected(transaction)"
                :binary="true"
                @click.stop="toggleSelection(transaction)"
              />
              <div class="card-date">
                <i class="pi pi-calendar" />
                <span>{{ transaction.date }}</span>
              </div>
            </div>

            <div class="card-header-right">
              <Button
                v-if="transaction.isPreview"
                v-tooltip="'Valider'"
                class="validate-button-mobile"
                icon="pi pi-check"
                text
                rounded
                severity="success"
                size="small"
                @click.stop="onConfirmPreview(transaction)"
              />
              <Button
                v-tooltip="'Modifier'"
                class="edit-button-mobile"
                icon="pi pi-pencil"
                text
                rounded
                size="small"
                @click.stop="onEditTransaction({ data: transaction })"
              />
            </div>
          </div>

          <!-- Corps de la carte -->
          <div class="card-body">
            <div class="card-label">
              <span class="label-text">{{ transaction.label }}</span>
              <i v-if="transaction.isPreview" class="pi pi-clock preview-badge" />
            </div>

            <div class="card-amount-section">
              <div class="card-amount" :class="[transaction.isIncome ? 'income' : 'expense']">
                <span class="amount-value">
                  {{ transaction.isIncome ? '+' : '-' }}
                  {{ Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2) }} €
                </span>
              </div>

              <Tag
                :value="transaction.tagDTO.label"
                :style="getTagStyle(transaction.tagDTO.colorDTO)"
                class="card-tag"
              />
            </div>
          </div>

          <!-- Indicateur de sélection -->
          <div v-if="isSelected(transaction)" class="selection-indicator">
            <i class="pi pi-check" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Dialogs -->
  <TransactionCreationDialog
    :visible="isCreationDialogVisible"
    title="Nouvelle transaction"
    :digit-placeholder="currentTransaction.value"
    :transaction-placeholder="currentTransaction"
    @cancel-creation="isCreationDialogVisible = false"
    @create-transaction="bookTransaction"
  />

  <TransactionCreationDialog
    :visible="isEditDialogVisible"
    title="Modifier la transaction"
    :digit-placeholder="currentTransaction.value"
    :transaction-placeholder="currentTransaction"
    button-title="Mettre à jour"
    @cancel-creation="isEditDialogVisible = false"
    @create-transaction="applyEditTransaction"
  />

  <CsvImportDialog
    ref="csvImportDialogRef"
    :booklet-id="bookletData.id"
    :month="bookletData.month"
    :year="bookletData.year"
    @import-success="onCsvImportSuccess"
  />
</template>

<style scoped lang="scss">
.booklet-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 1.25rem;
  gap: 0;
  background: linear-gradient(135deg, var(--bg-gradient-from) 0%, var(--bg-gradient-to) 100%);

  @media (max-width: 768px) {
    height: auto;
    min-height: 100vh;
    padding: 0.75rem;
    padding-bottom: 2rem;
  }
}

// ===== HEADER =====
.page-header {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 2px 12px var(--shadow-md);
  border: 1px solid var(--card-border);
  margin-bottom: 1.25rem;

  @media (max-width: 1024px) {
    padding: 1rem;
    border-radius: 14px;
  }

  @media (max-width: 768px) {
    padding: 0.875rem;
    border-radius: 12px;
    margin-bottom: 1rem;
  }
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;

  @media (max-width: 1024px) {
    flex-direction: column;
    align-items: stretch;
    gap: 1.25rem;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 0;

  @media (max-width: 768px) {
    gap: 0.75rem;
  }
}

.back-button {
  color: var(--primary);
  flex-shrink: 0;
  width: 38px;
  height: 38px;

  &:hover {
    background: rgba(130, 42, 204, 0.1);
  }

  @media (max-width: 768px) {
    width: 36px;
    height: 36px;
  }
}

.booklet-info {
  flex: 1;
  min-width: 0;

  h1 {
    font-size: 1.5rem;
    font-weight: 800;
    background: linear-gradient(135deg, var(--primary), var(--primary-2));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 0.375rem 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    @media (max-width: 768px) {
      font-size: 1.25rem;
      margin-bottom: 0.25rem;
    }
  }
}

.meta-info {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;

  @media (max-width: 768px) {
    gap: 0.625rem;
  }
}

.meta-item {
  display: inline-flex;
  align-items: center;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-secondary);
  position: relative;

  @media (max-width: 768px) {
    font-size: 0.75rem;
  }

  &:not(:last-child)::after {
    content: '•';
    margin-left: 1rem;
    color: var(--text-muted);

    @media (max-width: 768px) {
      margin-left: 0.625rem;
    }
  }

  &.preview {
    color: #d97706;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-shrink: 0;

  @media (max-width: 1024px) {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }
}

.balance-compact {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.75rem 1.25rem;
  background: linear-gradient(135deg, var(--bg-tertiary), var(--bg-secondary));
  border-radius: 12px;
  border: 1px solid var(--border-color);

  @media (max-width: 1024px) {
    justify-content: space-around;
  }

  @media (max-width: 768px) {
    padding: 0.625rem 1rem;
    gap: 1rem;
  }
}

.balance-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;

  @media (max-width: 768px) {
    flex: 1;
  }
}

.balance-label-compact {
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);

  @media (max-width: 768px) {
    font-size: 0.625rem;
  }
}

.balance-value-compact {
  font-size: 1.25rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;

  @media (max-width: 768px) {
    font-size: 1.125rem;
  }

  &.real {
    color: var(--primary);
  }

  &.preview {
    color: #d97706;
  }
}

.balance-divider {
  width: 1px;
  height: 2.5rem;
  background: linear-gradient(to bottom, transparent, var(--border-color), transparent);

  @media (max-width: 768px) {
    height: 2.25rem;
  }
}

.date-filters-compact {
  display: flex;
  gap: 0.75rem;

  @media (max-width: 768px) {
    gap: 0.5rem;
  }

  :deep(.p-dropdown),
  :deep(.p-select),
  :deep(.p-calendar) {
    border: 1px solid var(--border-color);
    border-radius: 10px;
    background: transparent; // hérite de var(--card-bg) de l'en-tête
    min-height: 38px;

    @media (max-width: 768px) {
      border-radius: 8px;
      min-height: 36px;
    }

    &:hover {
      border-color: var(--primary);
      background: var(--card-hover-bg);
    }

    &:focus-within {
      border-color: var(--primary);
      box-shadow: 0 0 0 2px rgba(130, 42, 204, 0.1);
      background: transparent;
    }

    .p-inputtext {
      background: transparent !important;
      color: var(--text-primary) !important;
      font-weight: 600;
      font-size: 0.875rem;
      padding: 0.5rem 0.75rem;
      border: none !important;
    }

    .p-dropdown-label,
    .p-select-label {
      color: var(--text-primary) !important;
      background: transparent !important;
    }

    .p-dropdown-trigger,
    .p-datepicker-trigger,
    .p-select-trigger {
      color: var(--text-secondary) !important;
      background: transparent !important;
    }

    .p-icon {
      color: var(--text-secondary) !important;
    }
  }

  // Styles spécifiques pour les panneaux dropdown/calendar
  :deep(.p-dropdown-panel),
  :deep(.p-select-panel),
  :deep(.p-datepicker) {
    background: var(--card-bg) !important;
    border: 1px solid var(--border-color) !important;
    box-shadow: 0 4px 16px var(--shadow-md) !important;

    .p-dropdown-items .p-dropdown-item {
      color: var(--text-primary) !important;

      &:hover {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }

    // Items du Select (PrimeVue v4)
    .p-select-list .p-select-option {
      color: var(--text-primary) !important;

      &:hover,
      &.p-focus {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-selected,
      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }

    .p-yearpicker .p-yearpicker-year {
      color: var(--text-primary) !important;

      &:hover {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }

    .p-datepicker-header {
      background: var(--bg-tertiary) !important;
      color: var(--text-primary) !important;
      border-bottom: 1px solid var(--border-color) !important;

      .p-datepicker-title {
        color: var(--text-primary) !important;
      }

      button {
        color: var(--text-primary) !important;

        &:hover {
          background: var(--card-hover-bg) !important;
        }
      }
    }
  }
}

.month-dropdown-compact {
  min-width: 120px;

  @media (max-width: 768px) {
    flex: 1;
    min-width: unset;
  }

  :deep(.p-dropdown),
  :deep(.p-select) {
    background: transparent !important; // hérite du fond de la carte
    border: 1px solid var(--border-color) !important;

    &:hover {
      background: var(--card-hover-bg) !important;
      border-color: var(--primary) !important;
    }

    &:focus,
    &:focus-within {
      border-color: var(--primary) !important;
      box-shadow: 0 0 0 2px rgba(130, 42, 204, 0.1) !important;
      background: transparent !important;
    }
  }

  :deep(.p-inputtext),
  :deep(input) {
    background: transparent !important;
    color: var(--text-primary) !important;
    font-weight: 600 !important;
    border: none !important;
  }

  :deep(.p-dropdown-label),
  :deep(.p-select-label) {
    color: var(--text-primary) !important;
    font-weight: 600 !important;
    background: transparent !important;
  }

  :deep(.p-dropdown-trigger),
  :deep(.p-select-trigger) {
    color: var(--text-secondary) !important;
    background: transparent !important;
  }

  :deep(.p-icon) {
    color: var(--text-secondary) !important;
  }

  // Cibler spécifiquement tous les éléments enfants
  :deep(*) {
    &:not(.p-dropdown-items):not(.p-dropdown-item):not(.p-select-list):not(.p-select-option) {
      background: transparent;
    }
  }
}

.year-picker-compact {
  min-width: 130px;

  @media (max-width: 768px) {
    flex: 1;
    min-width: unset;
  }

  // rendre l'input et le conteneur clairement cliquables
  :deep(.p-calendar) {
    cursor: pointer;
    transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
    border-radius: 14px !important; // plus arrondi
    min-height: 44px; // un peu plus haut
  }

  :deep(.p-inputtext) {
    font-size: 0.95rem; // légèrement plus grand
    padding: 0.6rem 0.75rem; // plus de padding
    text-align: center;
    background: transparent !important;
    color: var(--text-primary) !important;
    border: none !important;
    font-weight: 700; // contraste perçu et affordance
    cursor: pointer;

    &::placeholder {
      color: var(--text-secondary);
      opacity: 0.9;
      font-weight: 600;
    }
  }

  :deep(.p-datepicker-trigger) {
    color: var(--text-secondary) !important;
    background: transparent !important;
    cursor: pointer;
  }
}

// ===== ACTIONS =====
.main-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;

  @media (max-width: 768px) {
    gap: 0.75rem;
    margin-bottom: 1rem;
  }
}

.action-buttons {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;

  @media (max-width: 768px) {
    width: 100%;
    gap: 0.5rem;

    button {
      flex: 1;
      font-size: 0.8125rem;
      padding: 0.5rem 0.875rem;

      :deep(.p-button-label) {
        font-weight: 600;
      }
    }
  }
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  border: none;
  font-weight: 600;
  box-shadow: 0 2px 8px var(--shadow-purple);
  transition: all 0.3s ease;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;

  &:hover {
    background: linear-gradient(135deg, var(--primary-2), var(--primary-3));
    box-shadow: 0 4px 12px var(--shadow-purple);
    transform: translateY(-1px);
  }
}

.btn-secondary {
  background: var(--card-bg);
  color: #d97706;
  border: 1.5px solid #f59e0b;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;

  &:hover {
    background: rgba(245, 158, 11, 0.1);
    border-color: #d97706;
    box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
    transform: translateY(-1px);
  }
}

.btn-csv-import {
  background: var(--card-bg);
  color: #0891b2;
  border: 1.5px solid #06b6d4;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;

  &:hover {
    background: rgba(8, 145, 178, 0.1);
    border-color: #0891b2;
    box-shadow: 0 2px 8px rgba(8, 145, 178, 0.2);
    transform: translateY(-1px);
  }
}

.btn-csv-export {
  background: var(--card-bg);
  color: #059669;
  border: 1.5px solid #10b981;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;

  &:hover {
    background: rgba(5, 150, 105, 0.1);
    border-color: #059669;
    box-shadow: 0 2px 8px rgba(5, 150, 105, 0.2);
    transform: translateY(-1px);
  }
}

.delete-button {
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
  transition: all 0.3s ease;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;

  &:hover {
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.25);
    transform: translateY(-1px);
  }

  @media (max-width: 768px) {
    width: 100%;
  }
}

// ===== TABLE (DESKTOP) =====
.transactions-table {
  flex: 1;
  background: var(--card-bg);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px var(--shadow-purple);
  border: 1px solid var(--card-border);

  :deep(.p-datatable) {
    .p-datatable-thead > tr > th {
      background: linear-gradient(135deg, var(--primary), var(--primary-2));
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
      border-bottom: 1px solid var(--border-color);
      background: var(--card-bg);

      &:hover {
        background: var(--card-hover-bg);
      }

      &.p-highlight {
        background: linear-gradient(135deg, rgba(130, 42, 204, 0.1), rgba(101, 30, 158, 0.1));
        border-left: 3px solid var(--primary);
      }

      &.preview-row {
        background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.05));
        border-left: 4px solid #f59e0b;

        &:hover {
          background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.1));
        }

        &.p-highlight {
          background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(217, 119, 6, 0.15));
          border-left: 4px solid #d97706;
        }
      }

      > td {
        padding: 16px;
        border: none;
        color: var(--text-primary);
      }
    }

    .p-checkbox {
      .p-checkbox-box {
        border-color: var(--primary);

        &.p-highlight {
          background: linear-gradient(135deg, var(--primary), var(--primary-2));
          border-color: var(--primary);
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
    color: var(--text-muted);
    margin-bottom: 16px;
  }

  h3 {
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0 0 8px 0;
  }

  p {
    font-size: 1rem;
    color: var(--text-secondary);
    margin: 0 0 24px 0;
  }
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-weight: 500;

  i {
    font-size: 0.875rem;
    color: var(--primary);
  }
}

.label-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.transaction-label {
  font-weight: 600;
  color: var(--text-primary);
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
  color: var(--text-muted);
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

// ===== MOBILE TRANSACTIONS =====
.transactions-mobile {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  @media (max-width: 768px) {
    overflow: visible;
    flex: none;
  }
}

.empty-state-mobile {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: var(--card-bg);
  border-radius: 16px;
  box-shadow: 0 4px 20px var(--shadow-purple);
  border: 1px solid var(--card-border);

  i {
    font-size: 3rem;
    color: var(--text-muted);
    margin-bottom: 16px;
  }

  h3 {
    font-size: 1.125rem;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0 0 8px 0;
  }

  p {
    font-size: 0.875rem;
    color: var(--text-secondary);
    margin: 0 0 20px 0;
  }
}

.transaction-cards {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 768px) {
    overflow-y: visible;
    flex: none;
    gap: 16px;
    padding: 0;
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(130, 42, 204, 0.2);
    border-radius: 3px;

    &:hover {
      background: rgba(130, 42, 204, 0.4);
    }
  }
}

.transaction-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 12px var(--shadow-purple);
  border: 2px solid var(--card-border);
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  @media (max-width: 768px) {
    padding: 18px;
    border-radius: 18px;
    box-shadow: 0 3px 15px var(--shadow-md);
  }

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(180deg, var(--primary), var(--primary-2));
    transition: width 0.3s ease;

    @media (max-width: 768px) {
      width: 5px;
    }
  }

  &.preview-card {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.05));
    border-color: rgba(245, 158, 11, 0.2);

    &::before {
      background: linear-gradient(180deg, #f59e0b, #d97706);
    }

    .preview-badge {
      color: #f59e0b;
      font-size: 1rem;
      animation: pulse 2s ease-in-out infinite;
    }
  }

  &.selected-card {
    border-color: var(--primary);
    background: var(--card-hover-bg);
    box-shadow: 0 4px 16px var(--shadow-purple);

    &::before {
      width: 6px;
    }

    @media (max-width: 768px) {
      box-shadow: 0 5px 20px var(--shadow-purple);
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);

  @media (max-width: 768px) {
    margin-bottom: 14px;
    padding-bottom: 14px;
  }
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  @media (max-width: 768px) {
    gap: 14px;
  }
}

.card-date {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;

  @media (max-width: 768px) {
    font-size: 0.9rem;
    gap: 7px;
  }

  i {
    font-size: 0.875rem;
    color: var(--primary);

    @media (max-width: 768px) {
      font-size: 0.95rem;
    }
  }
}

.card-header-right {
  display: flex;
  gap: 4px;

  @media (max-width: 768px) {
    gap: 6px;
  }
}

.validate-button-mobile {
  color: #10b981;

  &:hover {
    background: rgba(16, 185, 129, 0.15);
  }

  @media (max-width: 768px) {
    :deep(.p-button-icon) {
      font-size: 1.1rem;
    }
  }
}

.edit-button-mobile {
  color: var(--primary);

  &:hover {
    background: rgba(130, 42, 204, 0.15);
  }

  @media (max-width: 768px) {
    :deep(.p-button-icon) {
      font-size: 1.1rem;
    }
  }
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 768px) {
    gap: 14px;
  }
}

.card-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.label-text {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
  flex: 1;
  word-break: break-word;

  @media (max-width: 768px) {
    font-size: 1.05rem;
    line-height: 1.4;
  }
}

.card-amount-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

.card-amount {
  flex: 1;

  @media (max-width: 768px) {
    width: 100%;
  }

  .amount-value {
    font-size: 1.5rem;
    font-weight: 800;
    font-family: 'Courier New', monospace;

    @media (max-width: 768px) {
      font-size: 1.75rem;
    }
  }

  &.expense .amount-value {
    color: #ef4444;
  }

  &.income .amount-value {
    color: #10b981;
  }
}

.card-tag {
  flex-shrink: 0;
  font-size: 0.75rem;
  padding: 4px 10px;

  @media (max-width: 768px) {
    font-size: 0.8rem;
    padding: 6px 12px;
    align-self: flex-start;
  }
}

.selection-indicator {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px var(--shadow-purple);
  animation: scaleIn 0.2s ease;

  @media (max-width: 768px) {
    width: 32px;
    height: 32px;
    top: 14px;
    right: 14px;
  }

  i {
    color: white;
    font-size: 0.875rem;

    @media (max-width: 768px) {
      font-size: 1rem;
    }
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

// Checkbox customization
:deep(.p-checkbox) {
  .p-checkbox-box {
    border-color: var(--primary);
    width: 22px;
    height: 22px;

    &.p-highlight {
      background: linear-gradient(135deg, var(--primary), var(--primary-2));
      border-color: var(--primary);
    }
  }
}
</style>

<style lang="scss">
// Styles globaux pour les panneaux dropdown et calendar (non-scoped car montés hors du composant)
.p-dropdown-panel,
.p-select-panel {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: 0 4px 16px var(--shadow-md) !important;

  .p-dropdown-header {
    background: var(--bg-tertiary) !important;
    border-bottom: 1px solid var(--border-color) !important;
  }

  .p-dropdown-items {
    background: var(--card-bg) !important;

    .p-dropdown-item {
      color: var(--text-primary) !important;
      background: transparent !important;

      &:hover {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }

      &.p-focus {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }
    }
  }

  // Liste du Select
  .p-select-list {
    background: var(--card-bg) !important;

    .p-select-option {
      color: var(--text-primary) !important;
      background: transparent !important;

      &:hover,
      &.p-focus {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-selected,
      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }
  }

  .p-dropdown-empty-message {
    color: var(--text-secondary) !important;
    background: var(--card-bg) !important;
  }
}

// Styles globaux pour le datepicker/calendar
.p-datepicker {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: 0 4px 16px var(--shadow-md) !important;

  .p-datepicker-header {
    background: var(--bg-tertiary) !important;
    color: var(--text-primary) !important;
    border-bottom: 1px solid var(--border-color) !important;

    .p-datepicker-title {
      color: var(--text-primary) !important;
    }

    button {
      color: var(--text-primary) !important;

      &:hover {
        background: var(--card-hover-bg) !important;
      }
    }
  }

  .p-yearpicker {
    .p-yearpicker-year {
      color: var(--text-primary) !important;

      &:hover {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }
  }

  .p-monthpicker {
    .p-monthpicker-month {
      color: var(--text-primary) !important;

      &:hover {
        background: var(--card-hover-bg) !important;
        color: var(--text-primary) !important;
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
        color: white !important;
      }
    }
  }
}
</style>
