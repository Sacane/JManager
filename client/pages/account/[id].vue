<script setup lang="ts">
import type { AxiosError } from 'axios'
import { useConfirm } from 'primevue/useconfirm'
import useCsvImport from '~/composables/useCsvImport'
import useTransaction from '~/composables/useTransaction'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import { capitalizeFirst, getTagStyle } from '~/utils/util'

definePageMeta({ layout: 'sidebar-layout' })

const { findBalancesByIdMonthAndYear, findTransactionsByIdMonthAndYear } = useBooklet()
const route = useRoute()
const toast = useJToast()
const confirm = useConfirm()

const { englishMonth, translate, monthFromNumber, numberFromMonth } = useDate()
const tag = useTag()
const { deleteTransaction, confirmPreviewTransaction, saveTransaction, editTransaction, findTransactionById } = useTransaction()
const { downloadCsvExport } = useCsvImport()
const { isScopeLoading, withLoading } = useLoading()

const selectedSheets = ref<TransactionCreationDTO[]>([])
const actualSheets = ref<TransactionCreationDTO[]>([])
const tags = ref<TagDTO[]>([])

const isCreationDialogVisible = ref(false)
const isEditDialogVisible = ref(false)
const isMobile = ref(false)
const csvImportDialogRef = ref<any>(null)
const isMobileMenuOpen = ref(false)
const transactionFilter = ref<'all' | 'preview' | 'confirmed'>('all')
const isConfirmPreviewDialogVisible = ref(false)
const newAmountForPreview = ref<number | null>(null)
const newDateForPreview = ref<Date | null>(null)
const transactionToConfirm = ref<TransactionCreationDTO | null>(null)

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
  tagDTO: { tagId: undefined, label: '', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
  isPreview: false,
})

const displayMonth = computed({
  get: () => translate(bookletData.month),
  set: (value: string) => {
    const normalized = englishMonth(value) || value
    if (numberFromMonth(normalized) != null) {
      bookletData.month = normalized
    }
  },
})
const transactionsCount = computed(() => actualSheets.value.length)
const previewTransactionsCount = computed(() => actualSheets.value.filter(t => t.isPreview).length)
const hasSelection = computed(() => selectedSheets.value.length > 0)
const selectedTransactionsAmount = computed(() => selectedSheets.value.reduce((total, transaction) => {
  const amount = Number.parseFloat(transaction?.value?.toString() ?? '0')
  return total + (transaction.isIncome ? amount : -amount)
}, 0))
const selectedTransactionsAmountLabel = computed(() => {
  const amount = selectedTransactionsAmount.value
  const formattedAmount = Math.abs(amount).toLocaleString('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

  if (amount === 0) return '0,00 €'

  return `${amount > 0 ? '+' : '-'}${formattedAmount} €`
})

const displayLabel = computed(() => capitalizeFirst(bookletData.label))
const loadBookletScope = LOADING_SCOPES.accountDetails.load
const bookTransactionScope = LOADING_SCOPES.accountDetails.createTransaction
const editTransactionScope = LOADING_SCOPES.accountDetails.editTransaction
const fetchTransactionScope = LOADING_SCOPES.accountDetails.fetchTransaction
const deleteTransactionScope = LOADING_SCOPES.accountDetails.deleteTransaction
const confirmPreviewScope = LOADING_SCOPES.accountDetails.confirmPreview
const exportCsvScope = LOADING_SCOPES.accountDetails.exportCsv
const isBookletLoading = computed(() => isScopeLoading(loadBookletScope))
const isBookTransactionLoading = computed(() => isScopeLoading(bookTransactionScope))
const isEditTransactionLoading = computed(() => isScopeLoading(editTransactionScope))
const isFetchTransactionLoading = computed(() => isScopeLoading(fetchTransactionScope))
const isDeleteTransactionLoading = computed(() => isScopeLoading(deleteTransactionScope))
const isConfirmPreviewLoading = computed(() => isScopeLoading(confirmPreviewScope))
const isExportCsvLoading = computed(() => isScopeLoading(exportCsvScope))
const isAnyActionLoading = computed(() =>
  isBookletLoading.value
  || isBookTransactionLoading.value
  || isEditTransactionLoading.value
  || isFetchTransactionLoading.value
  || isDeleteTransactionLoading.value
  || isConfirmPreviewLoading.value
  || isExportCsvLoading.value,
)

const filteredTransactions = computed(() => {
  if (transactionFilter.value === 'preview') {
    return actualSheets.value.filter(t => t.isPreview)
  } else if (transactionFilter.value === 'confirmed') {
    return actualSheets.value.filter(t => !t.isPreview)
  }
  return actualSheets.value
})

function asDisplayableTransaction(transaction: TransactionResultDTO): any {
  const fallbackTag: TagDTO = {
    tagId: undefined,
    label: 'Aucune',
    isDefault: true,
    colorDTO: { red: 255, green: 255, blue: 255 },
  }

  return {
    ...transaction,
    id: transaction.id,
    expensesRepresentation: !transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    incomeRepresentation: transaction.isIncome ? `${Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2)} €` : '-',
    date: transaction.date,
    tagDTO: transaction.tagDTO ?? fallbackTag,
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
  await withLoading(async () => {
    try {
      const accountId = (route.params as any)?.id as string
      const month = numberFromMonth(bookletData.month) as number

      const [balances, transactionsRes] = await Promise.all([
        findBalancesByIdMonthAndYear(accountId, month, bookletData.year),
        findTransactionsByIdMonthAndYear(accountId, month, bookletData.year),
      ])

      bookletData.label = balances.label
      bookletData.id = accountId
      bookletData.realSold = Number.parseFloat(balances.realSold)
      bookletData.previewSold = Number.parseFloat(balances.previewSold)

      actualSheets.value = transactionsRes.transactions
        .map(asDisplayableTransaction)
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    } catch (err) {
      toast.errorAxios(err as AxiosError)
      console.error(err)
    }
  }, loadBookletScope)
}

async function retrieveTags() {
  try {
    tags.value = await tag.getAllTags()
  } catch (err) {
    toast.errorAxios(err as AxiosError)
  }
}

function onMonthChange() {
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
  await withLoading(async () => {
    try {
      const result = await saveTransaction(bookletData.label, transaction)

      const newTransaction = asDisplayableTransaction(result)
      actualSheets.value.push(newTransaction)
      actualSheets.value.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

      await loadBookletData()

      isCreationDialogVisible.value = false
      resetTransaction()
      toast.success('Transaction enregistrée avec succès')
    } catch (err) {
      toast.errorAxios(err as AxiosError)
    }
  }, bookTransactionScope)
}

async function onEditTransaction(event: any) {
  await withLoading(async () => {
    try {
      if (!event?.data?.id) {
        toast.warn('Cette transaction est virtuelle. Validez-la pour la créer avant modification.')
        return
      }

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
  }, fetchTransactionScope)
}

async function applyEditTransaction(transaction: TransactionCreationDTO) {
  await withLoading(async () => {
    try {
      const result: TransactionResultDTO = await editTransaction(transaction, bookletData.id)

      const index = actualSheets.value.findIndex(item => item?.id === result.id)
      if (index !== -1) {
        actualSheets.value[index] = asDisplayableTransaction(result)
      }

      await loadBookletData()

      isEditDialogVisible.value = false
      resetTransaction()
      toast.success('Transaction mise à jour avec succès')
    } catch (err) {
      toast.errorAxios(err as AxiosError)
    }
  }, editTransactionScope)
}

async function confirmDelete() {
  await withLoading(async () => {
    try {
      const idsToDelete = selectedSheets.value
        .map(sheet => sheet.id)
        .filter((id): id is string => id != null)

      if (idsToDelete.length === 0) {
        toast.warn('La sélection contient uniquement des transactions virtuelles non supprimables.')
        selectedSheets.value = []
        return
      }

      const res = await deleteTransaction(
        bookletData.id,
        idsToDelete,
      )

      // Update list locally (no reload)
      const deleted = new Set(res.deletedIds)
      actualSheets.value = actualSheets.value.filter(t => !deleted.has(t.id as string))
      selectedSheets.value = []

      await loadBookletData()

      toast.success('Transactions supprimées avec succès')
    } catch (err) {
      toast.errorAxios(err as AxiosError)
    }
  }, deleteTransactionScope)
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

async function confirmPreview() {
  const transaction = transactionToConfirm.value
  if (!transaction) return
  await withLoading(async () => {
    try {
      const finalAmount = newAmountForPreview.value ?? transaction.value
      const baseDate = transaction.date instanceof Date ? transaction.date : new Date(transaction.date)
      const finalDate = newDateForPreview.value ?? baseDate

      if (finalAmount == null) {
        toast.warn('Veuillez renseigner un montant pour valider cette transaction.')
        return
      }

      if (Number.isNaN(finalDate.getTime())) {
        toast.warn('Veuillez renseigner une date valide pour valider cette transaction.')
        return
      }

      if (transaction.id) {
        const result = await confirmPreviewTransaction(bookletData.id, transaction.id, newAmountForPreview.value, finalDate)

        const index = actualSheets.value.findIndex(item => item?.id === result.id)
        if (index !== -1) {
          actualSheets.value[index] = asDisplayableTransaction(result)
        }
      } else {
        await saveTransaction(bookletData.label, {
          ...transaction,
          id: null,
          isPreview: false,
          value: finalAmount,
          date: finalDate,
        })
      }

      await loadBookletData()

      toast.success('Transaction validée avec succès')
    } catch (err) {
      toast.errorAxios(err as AxiosError)
    } finally {
      isConfirmPreviewDialogVisible.value = false
      newAmountForPreview.value = null
      newDateForPreview.value = null
      transactionToConfirm.value = null
    }
  }, confirmPreviewScope)
}

function onConfirmPreview(transaction: TransactionCreationDTO) {
  transactionToConfirm.value = transaction
  const parsedDate = transaction.date instanceof Date ? transaction.date : new Date(transaction.date)
  newDateForPreview.value = Number.isNaN(parsedDate.getTime()) ? null : parsedDate
  isConfirmPreviewDialogVisible.value = true
}

function rowClass(row: TransactionCreationDTO): string {
  if (row.isPreview) return 'preview-row'
  return ''
}

function toggleSelection(transaction: TransactionCreationDTO) {
  const index = selectedSheets.value.findIndex(t => t.id === transaction.id)
  if (index === -1) selectedSheets.value.push(transaction)
  else selectedSheets.value.splice(index, 1)
}

function isSelected(transaction: TransactionCreationDTO): boolean {
  return selectedSheets.value.some(t => t.id === transaction.id)
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

function openCsvImportFromMenu() {
  isMobileMenuOpen.value = false
  openCsvImportDialog()
}

function openCsvExportFromMenu() {
  isMobileMenuOpen.value = false
  openCsvExportDialog()
}

function openCsvImportDialog() {
  csvImportDialogRef.value?.openDialog()
}

function openCsvExportDialog() {
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
      await withLoading(async () => {
        try {
          const transactionIds = nonPreviewTransactions.map(t => t.id).filter(id => id != null) as string[]
          const filename = `transactions_${bookletData.label.replace(/\s+/g, '_')}_${bookletData.month}_${bookletData.year}.csv`
          await downloadCsvExport(transactionIds, filename)
          toast.success('Fichier CSV téléchargé avec succès !')
        } catch (err) {
          console.error('Erreur lors de l\'export CSV:', err)
          toast.errorAxios(err as AxiosError)
        }
      }, exportCsvScope)
    },
  })
}

function onCsvImportSuccess(result: CsvImportResultDTO) {
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
},
)
</script>

<template>
  <ConfirmDialog />

  <div class="flex flex-col min-h-screen bg-gradient-to-br from-[var(--bg-gradient-from)] to-[var(--bg-gradient-to)] py-5 md:(py-3 pb-8)">
    <div class="w-full max-w-7xl mx-auto px-5 md:px-6 lg:px-8">
      <div class="bg-[var(--card-bg)] rounded-2xl p-5 shadow border border-[var(--card-border)] overflow-hidden mb-5 lg:(p-4 rounded-xl) md:(p-3 rounded-lg mb-4)">
        <div class="flex flex-col md:flex-row justify-between items-center gap-4 md:gap-8">
          <div class="flex items-center gap-4 flex-1 min-w-0 md:gap-3">
            <Button class="text-[var(--primary)] w-9 h-9 rounded-full grid place-items-center hover:bg-[rgba(130,42,204,0.1)]" icon="pi pi-arrow-left" text rounded @click="navigateTo('/account')" />
            <div class="flex-1 min-w-0">
              <h1 class="text-2xl font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0 md:(text-xl mb-1)">
                {{ displayLabel }}
              </h1>
              <div class="flex gap-4 flex-wrap md:gap-2.5">
                <span class="inline-flex items-center text-sm font-semibold text-[var(--text-secondary)]">{{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}</span>
                <span v-if="previewTransactionsCount > 0" class="text-amber-600 inline-flex items-center text-sm font-semibold">{{ previewTransactionsCount }} en attente</span>
              </div>
            </div>
          </div>
          <!-- Right: balances + filters -->
          <div class="flex flex-col items-stretch gap-3 shrink-0 w-full md:(w-auto flex-row items-center gap-6)">
            <div class="flex items-center justify-between w-full md:w-auto gap-4 p-3 bg-gradient-to-br from-[var(--bg-tertiary)] to-[var(--bg-secondary)] rounded-xl border border-[var(--card-border)] md:(p-2.5 gap-4)">
              <div class="flex flex-col gap-1">
                <span class="text-[0.69rem] font-semibold uppercase tracking-wider text-[var(--text-tertiary)] md:text-2xs">Réel</span><span class="text-xl font-extrabold text-[var(--primary)] md:text-lg">{{ bookletData.realSold.toFixed(2) }} €</span>
              </div>
              <div class="w-px h-10 md:h-9 bg-gradient-to-b from-transparent via-[var(--border-color)] to-transparent" />
              <div class="flex flex-col gap-1">
                <span class="text-[0.69rem] font-semibold uppercase tracking-wider text-[var(--text-tertiary)] md:text-2xs">Prévisionnel</span><span class="text-xl font-extrabold text-amber-600 md:text-lg">{{ bookletData.previewSold.toFixed(2) }} €</span>
              </div>
            </div>

            <div class="flex w-full md:w-auto gap-3 md:gap-2">
              <Select
                v-model="displayMonth"
                :options="useDate().months.map(u => translate(u))"
                placeholder="Mois"
                class="flex-1 min-w-0 w-full md:(flex-none min-w-[140px] w-auto) border-1 rounded-lg bg-transparent"
                @change="onMonthChange"
              />
              <DatePicker
                v-model="bookletData.dateYear"
                view="year"
                date-format="yy"
                class="flex-1 min-w-0 w-full md:(flex-none min-w-[140px] w-auto) rounded-[14px] min-h-[46px] cursor-pointer bg-transparent"
                placeholder="Année"
                :show-icon="true"
                icon-display="input"
                @date-select="onYearChange"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Filtres de transactions -->
      <div class="flex items-center gap-2 mb-4 overflow-x-auto pb-2 md:gap-3">
        <span class="text-sm font-semibold text-[var(--text-secondary)] whitespace-nowrap mr-1">Afficher :</span>
        <button
          class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap"
          :class="transactionFilter === 'all'
            ? 'bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-[0_2px_8px_rgba(130,42,204,0.25)]'
            : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-[var(--primary)]'"
          @click="transactionFilter = 'all'"
        >
          <i class="pi pi-list mr-2" />
          Tout ({{ transactionsCount }})
        </button>
        <button
          class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap"
          :class="transactionFilter === 'confirmed'
            ? 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white shadow-[0_2px_8px_rgba(16,185,129,0.25)]'
            : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-emerald-600'"
          @click="transactionFilter = 'confirmed'"
        >
          <i class="pi pi-check-circle mr-2" />
          Confirmées ({{ transactionsCount - previewTransactionsCount }})
        </button>
        <button
          class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap"
          :class="transactionFilter === 'preview'
            ? 'bg-gradient-to-br from-amber-500 to-amber-600 text-white shadow-[0_2px_8px_rgba(245,158,11,0.25)]'
            : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-amber-600'"
          @click="transactionFilter = 'preview'"
        >
          <i class="pi pi-clock mr-2" />
          Prévisionnelles ({{ previewTransactionsCount }})
        </button>
      </div>

      <div class="flex flex-col gap-3 mb-5 md:mb-4">
        <div class="flex flex-col gap-3 lg:(flex-row justify-between items-center gap-6)">
          <div class="flex flex-col gap-2 md:(flex-row gap-3 flex-wrap)">
            <Button class="btn-primary w-full md:w-auto" icon="pi pi-plus" :label="isMobile ? 'Transaction' : 'Nouvelle transaction'" :disabled="isAnyActionLoading" @click="openCreationDialog" />
            <Button
              outlined
              class="w-full md:w-auto border-amber-500 text-amber-600 hover:bg-amber-500/10 font-semibold transition-all shadow-[0_2px_8px_rgba(245,158,11,0.15)] hover:shadow-[0_4px_12px_rgba(245,158,11,0.25)]"
              icon="pi pi-clock"
              :label="isMobile ? 'Prévisionnelle' : 'Transaction prévisionnelle'"
              :disabled="isAnyActionLoading"
              @click="openPreviewCreationDialog"
            />
          </div>

          <div class="flex flex-col gap-2 lg:(flex-row items-center gap-3 shrink-0)">
            <Button
              outlined
              class="csv-action-btn w-full lg:w-auto border-cyan-500 text-cyan-600 hover:bg-cyan-500/10 font-semibold transition-all shadow-[0_2px_8px_rgba(6,182,212,0.15)] hover:shadow-[0_4px_12px_rgba(6,182,212,0.25)]"
              icon="pi pi-file-import"
              :label="isMobile ? 'CSV' : 'Importer CSV'"
              :disabled="isAnyActionLoading"
              @click="openCsvImportDialog"
            />
            <Button
              outlined
              class="csv-action-btn w-full lg:w-auto border-emerald-500 text-emerald-600 hover:bg-emerald-500/10 font-semibold transition-all shadow-[0_2px_8px_rgba(16,185,129,0.15)] hover:shadow-[0_4px_12px_rgba(16,185,129,0.25)]"
              icon="pi pi-file-export"
              :label="isMobile ? 'Export' : 'Exporter CSV'"
              :loading="isExportCsvLoading"
              :disabled="isAnyActionLoading"
              @click="openCsvExportDialog"
            />
          </div>
        </div>

        <Transition name="fade">
          <div v-if="hasSelection" class="flex flex-col gap-3 lg:(flex-row items-center justify-end gap-3)">
            <div class="flex items-center justify-between gap-6 px-4 py-2.5 rounded-xl border border-[var(--card-border)] bg-[var(--card-bg)] shadow-sm lg:justify-start">
              <div class="flex items-center gap-2">
                <i class="pi pi-check-square text-[var(--primary)] text-sm" />
                <span class="text-sm font-semibold text-[var(--text-secondary)]">{{ selectedSheets.length }} transaction{{ selectedSheets.length > 1 ? 's' : '' }} sélectionnée{{ selectedSheets.length > 1 ? 's' : '' }}</span>
              </div>
              <div class="w-px h-6 bg-[var(--border-color)]" />
              <span class="text-base font-extrabold" :class="selectedTransactionsAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">
                {{ selectedTransactionsAmountLabel }}
              </span>
            </div>
            <Button class="w-full lg:w-auto" icon="pi pi-trash" :label="`Supprimer (${selectedSheets.length})`" severity="danger" :loading="isDeleteTransactionLoading" :disabled="isAnyActionLoading" @click="confirmDeleteButton" />
          </div>
        </Transition>
      </div>

      <div v-if="isBookletLoading" class="mb-4 flex items-center justify-center gap-2 rounded-xl border border-[var(--card-border)] bg-[var(--card-bg)] px-4 py-3 text-[var(--text-secondary)]">
        <i class="pi pi-spin pi-spinner" />
        <span>Chargement des transactions...</span>
      </div>

      <div v-if="!isMobile" class="flex-1 bg-[var(--card-bg)] rounded-2xl overflow-hidden border border-[var(--card-border)] shadow-lg">
        <DataTable
          v-model:selection="selectedSheets"
          :value="filteredTransactions"
          :row-class="rowClass"
          scrollable
          scroll-height="flex"
          selection-mode="multiple"
          :meta-key-selection="false"
          @row-dblclick="onEditTransaction"
        >
          <template #empty>
            <div class="text-center py-12">
              <i class="pi pi-inbox text-4xl text-[var(--text-muted)]" />
              <h3 class="text-xl font-bold text-[var(--text-primary)] mt-4 mb-2">
                Aucune transaction
              </h3>
              <p class="text-[var(--text-secondary)] mb-4">
                Commencez par créer votre première transaction
              </p>
              <Button class="btn-primary" icon="pi pi-plus" label="Créer une transaction" @click="openCreationDialog" />
            </div>
          </template>

          <Column selection-mode="multiple" :style="{ width: '3rem' }" />

          <Column field="date" header="Date" :sortable="true" :style="{ minWidth: '120px' }">
            <template #body="{ data }">
              <div class="flex items-center gap-2 text-[var(--text-secondary)] font-medium">
                <i class="pi pi-calendar text-[var(--primary)] text-sm" />
                <span>{{ data.date }}</span>
              </div>
            </template>
          </Column>

          <Column field="label" header="Libellé" :style="{ minWidth: '200px' }">
            <template #body="{ data }">
              <div class="flex items-center gap-2">
                <span class="font-semibold text-[var(--text-primary)]">{{ data.label }}</span>
                <i v-if="data.isPreview" class="pi pi-clock text-amber-500 text-sm" title="Transaction prévisionnelle" />
              </div>
            </template>
          </Column>

          <Column field="expensesRepresentation" header="Dépenses" :style="{ minWidth: '120px' }">
            <template #body="{ data }">
              <span v-if="!data.isIncome" class="font-extrabold text-red-500">{{ data.expensesRepresentation }}</span>
              <span v-else class="text-[var(--text-muted)] font-semibold">-</span>
            </template>
          </Column>

          <Column field="incomeRepresentation" header="Recettes" :style="{ minWidth: '120px' }">
            <template #body="{ data }">
              <span v-if="data.isIncome" class="font-extrabold text-emerald-500">{{ data.incomeRepresentation }}</span>
              <span v-else class="text-[var(--text-muted)] font-semibold">-</span>
            </template>
          </Column>

          <Column field="tagDTO" header="Catégorie" :style="{ minWidth: '150px' }">
            <template #body="{ data }">
              <Tag :value="data.tagDTO.label" :style="getTagStyle(data.tagDTO.colorDTO)" />
            </template>
          </Column>

          <Column header="Actions" :style="{ width: '140px', textAlign: 'center' }">
            <template #body="{ data }">
              <div class="flex items-center justify-center gap-1">
                <Button
                  v-if="data.id"
                  class="text-[var(--primary)] hover:bg-[rgba(130,42,204,0.15)]"
                  icon="pi pi-pencil"
                  text
                  rounded
                  size="small"
                  :disabled="isAnyActionLoading"
                  title="Modifier la transaction"
                  @click="onEditTransaction({ data })"
                />
                <Button
                  v-if="data.isPreview"
                  class="text-emerald-500 hover:bg-emerald-500/15"
                  icon="pi pi-check"
                  text
                  rounded
                  size="small"
                  severity="success"
                  :loading="isConfirmPreviewLoading"
                  :disabled="isAnyActionLoading"
                  title="Valider la transaction prévisionnel"
                  @click="onConfirmPreview(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </div>

      <div v-else class="flex-1 overflow-hidden flex flex-col md:(overflow-visible flex-none)">
        <div v-if="filteredTransactions.length === 0" class="flex-1 flex flex-col items-center justify-center p-10 text-center bg-[var(--card-bg)] rounded-2xl shadow-lg border border-[var(--card-border)]">
          <i class="pi pi-inbox text-4xl text-[var(--text-muted)]" />
          <h3 class="text-lg font-bold text-[var(--text-primary)] mt-4 mb-2">
            Aucune transaction
          </h3>
          <p class="text-[var(--text-secondary)] mb-4">
            {{ transactionFilter === 'preview' ? 'Aucune transaction prévisionnelle' : transactionFilter === 'confirmed' ? 'Aucune transaction confirmée' : 'Commencez par créer votre première transaction' }}
          </p>
          <Button class="btn-primary" icon="pi pi-plus" label="Créer" @click="openCreationDialog" />
        </div>

        <div v-else class="flex-1 overflow-y-auto p-1 flex flex-col gap-3 md:(overflow-visible flex-none gap-4 p-0)">
          <div
            v-for="(transaction, tIndex) in filteredTransactions"
            :key="transaction.id || `t-${tIndex}`"
            class="relative bg-[var(--card-bg)] rounded-xl p-4 shadow border-2 border-[var(--card-border)] transition-all overflow-hidden hover:shadow-lg active:scale-[0.98]"
            :class="[
              transaction.isPreview ? 'bg-gradient-to-br from-amber-500/10 to-amber-600/5 border-amber-400/20' : '',
              isSelected(transaction) ? 'border-[var(--primary)] bg-[var(--card-hover-bg)] shadow-lg' : '',
            ]"
            @click="toggleSelection(transaction)"
          >
            <div class="flex justify-between items-center mb-3 pb-3 border-b border-[var(--border-color)]">
              <div class="flex items-center gap-3 md:gap-3.5">
                <Checkbox :model-value="isSelected(transaction)" :binary="true" @click.stop="toggleSelection(transaction)" />
                <div class="flex items-center gap-1.5 text-[var(--text-secondary)] text-sm font-medium">
                  <i class="pi pi-calendar text-[var(--primary)] text-sm" />
                  <span>{{ transaction.date }}</span>
                </div>
              </div>
              <div class="flex gap-1.5 md:gap-1.5">
                <Button
                  v-if="transaction.id"
                  class="text-[var(--primary)] hover:bg-[rgba(130,42,204,0.15)]"
                  icon="pi pi-pencil"
                  text
                  rounded
                  size="small"
                  :disabled="isAnyActionLoading"
                  title="Modifier"
                  @click.stop="onEditTransaction({ data: transaction })"
                />
                <Button v-if="transaction.isPreview" class="text-emerald-500 hover:bg-emerald-500/15" icon="pi pi-check" text rounded severity="success" size="small" :loading="isConfirmPreviewLoading" :disabled="isAnyActionLoading" title="Valider" @click.stop="onConfirmPreview(transaction)" />
              </div>
            </div>

            <div class="flex flex-col gap-3 md:gap-3.5">
              <div class="flex items-center justify-between gap-2">
                <span class="text-base font-bold text-[var(--text-primary)] md:(text-[1.05rem] leading-snug)">{{ transaction.label }}</span>
                <i v-if="transaction.isPreview" class="pi pi-clock text-amber-500 text-base" />
              </div>
              <div class="flex justify-between items-center gap-3 md:(flex-col items-start gap-2.5)">
                <div class="flex-1 w-full">
                  <span class="text-2xl font-extrabold font-mono" :class="transaction.isIncome ? 'text-emerald-500' : 'text-red-500'">
                    {{ transaction.isIncome ? '+' : '-' }} {{ Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2) }} €
                  </span>
                </div>
                <Tag :value="transaction.tagDTO.label" :style="getTagStyle(transaction.tagDTO.colorDTO)" class="text-sm px-2.5 py-1 md:(text-base px-3 py-1.5 self-start)" />
              </div>
            </div>

            <div v-if="isSelected(transaction)" class="absolute top-3 right-3 w-7 h-7 rounded-full grid place-items-center shadow bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] md:(w-8 h-8 top-3.5 right-3.5)">
              <i class="pi pi-check text-white text-sm md:text-base" />
            </div>
          </div>
        </div>
      </div>

      <TransactionCreationDialog
        :visible="isCreationDialogVisible"
        :loading="isBookTransactionLoading"
        :digit-placeholder="currentTransaction.value"
        :transaction-placeholder="currentTransaction"
        :title="isCreationDialogVisible ? (currentTransaction.isPreview ? 'Nouvelle transaction prévisionnelle' : 'Nouvelle transaction') : ''"
        @cancel-creation="isCreationDialogVisible = false"
        @create-transaction="bookTransaction"
      />

      <TransactionCreationDialog
        :visible="isEditDialogVisible"
        :loading="isEditTransactionLoading"
        :title="isEditDialogVisible ? (currentTransaction.isPreview ? 'Modifier la transaction prévisionnelle' : 'Modifier la transaction') : ''"
        :digit-placeholder="currentTransaction.value"
        :transaction-placeholder="currentTransaction"
        button-title="Mettre à jour"
        @cancel-creation="isEditDialogVisible = false"
        @create-transaction="applyEditTransaction"
      />

      <Dialog
        v-model:visible="isConfirmPreviewDialogVisible"
        modal
        header="Valider la transaction prévisionnelle"
        :style="{ width: '25rem' }"
        :pt="{
          root: { class: 'preview-confirm-root' },
          mask: { class: 'preview-confirm-mask' },
          header: { class: 'preview-confirm-header' },
          title: { class: 'preview-confirm-title' },
          closeButton: { class: 'preview-confirm-close-btn' },
          content: { class: 'preview-confirm-content' },
          footer: { class: 'preview-confirm-footer' },
        }"
      >
        <div v-if="transactionToConfirm" class="flex flex-col gap-4">
          <p>Voulez-vous valider cette transaction prévisionnelle ?</p>

          <div class="preview-confirm-summary p-3 rounded-lg border">
            <div class="flex justify-between items-center text-sm">
              <span class="font-semibold">Transaction</span>
              <span class="font-medium">{{ transactionToConfirm.label }}</span>
            </div>
            <div class="preview-confirm-summary-row flex justify-between items-center mt-2 pt-2 border-t">
              <span class="font-semibold">Montant de base</span>
              <span class="font-bold text-lg" :class="transactionToConfirm.isIncome ? 'text-emerald-500' : 'text-red-500'">
                {{ transactionToConfirm.isIncome ? '+' : '-' }} {{ transactionToConfirm.value }} €
              </span>
            </div>
            <div class="preview-confirm-summary-row flex justify-between items-center mt-2 pt-2 border-t">
              <span class="font-semibold">Date de base</span>
              <span class="font-medium">{{ transactionToConfirm.date }}</span>
            </div>
          </div>

          <p class="text-sm preview-confirm-help-text">
            Vous pouvez optionnellement spécifier un nouveau montant et une nouvelle date ci-dessous.
          </p>
          <div class="flex flex-col gap-3">
            <div class="flex flex-col gap-2">
              <label for="newAmount" class="font-semibold">Nouveau montant</label>
              <InputNumber id="newAmount" v-model="newAmountForPreview" mode="currency" currency="EUR" locale="fr-FR" placeholder="0.00" class="preview-confirm-field" />
            </div>
            <div class="flex flex-col gap-2">
              <label for="newDate" class="font-semibold">Nouvelle date</label>
              <DatePicker id="newDate" v-model="newDateForPreview" date-format="dd/mm/yy" show-icon icon-display="input" class="preview-confirm-field" />
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button type="button" label="Annuler" severity="secondary" :disabled="isConfirmPreviewLoading" @click="isConfirmPreviewDialogVisible = false" />
          <Button type="button" label="Valider" :loading="isConfirmPreviewLoading" :disabled="isConfirmPreviewLoading" @click="confirmPreview" />
        </div>
      </Dialog>

      <CsvImportDialog
        ref="csvImportDialogRef"
        :booklet-id="bookletData.id"
        :month="bookletData.month"
        :year="bookletData.year"
        @import-success="onCsvImportSuccess"
      />

      <Transition name="fab">
        <button
          v-if="isMobile"
          class="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-lg flex items-center justify-center z-40 transition-all duration-300 hover:shadow-xl hover:scale-110 active:scale-95"
          @click="toggleMobileMenu"
        >
          <i :class="isMobileMenuOpen ? 'pi pi-times text-xl' : 'pi pi-ellipsis-h text-xl'" />
        </button>
      </Transition>

      <Transition name="overlay">
        <div
          v-if="isMobile && isMobileMenuOpen"
          class="fixed inset-0 z-50 flex items-end justify-center backdrop-blur-md bg-black/30"
          @click="toggleMobileMenu"
        >
          <Transition name="menu-slide">
            <div
              v-if="isMobileMenuOpen"
              class="w-full max-w-md bg-[var(--card-bg)] rounded-t-3xl shadow-2xl p-6 mb-0"
              @click.stop
            >
              <div class="flex items-center justify-between mb-6">
                <h3 class="text-xl font-bold text-[var(--text-primary)] m-0">
                  Actions CSV
                </h3>
                <button
                  class="w-8 h-8 rounded-full flex items-center justify-center text-[var(--text-secondary)] hover:bg-[var(--card-hover-bg)] transition-colors"
                  @click="toggleMobileMenu"
                >
                  <i class="pi pi-times" />
                </button>
              </div>

              <div class="flex flex-col gap-3">
                <button
                  class="flex items-center gap-4 p-4 rounded-xl bg-gradient-to-br from-cyan-500/10 to-cyan-600/5 border-2 border-cyan-500/20 text-left transition-all hover:border-cyan-500/40 hover:shadow-lg active:scale-[0.98]"
                  @click="openCsvImportFromMenu"
                >
                  <div class="w-12 h-12 rounded-full bg-cyan-500/20 flex items-center justify-center shrink-0">
                    <i class="pi pi-file-import text-xl text-cyan-600" />
                  </div>
                  <div class="flex-1">
                    <div class="font-semibold text-[var(--text-primary)] mb-1">
                      Importer CSV
                    </div>
                    <div class="text-sm text-[var(--text-secondary)]">
                      Importer des transactions depuis un fichier
                    </div>
                  </div>
                  <i class="pi pi-chevron-right text-[var(--text-secondary)]" />
                </button>

                <button
                  class="flex items-center gap-4 p-4 rounded-xl bg-gradient-to-br from-emerald-500/10 to-emerald-600/5 border-2 border-emerald-500/20 text-left transition-all hover:border-emerald-500/40 hover:shadow-lg active:scale-[0.98]"
                  @click="openCsvExportFromMenu"
                >
                  <div class="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0">
                    <i class="pi pi-file-export text-xl text-emerald-600" />
                  </div>
                  <div class="flex-1">
                    <div class="font-semibold text-[var(--text-primary)] mb-1">
                      Exporter CSV
                    </div>
                    <div class="text-sm text-[var(--text-secondary)]">
                      Télécharger vos transactions en CSV
                    </div>
                  </div>
                  <i class="pi pi-chevron-right text-[var(--text-secondary)]" />
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped lang="scss">
:deep(.p-dropdown),
:deep(.p-select),
:deep(.p-calendar) {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
}
:deep(.p-inputtext),
:deep(.p-dropdown-label),
:deep(.p-select-label) {
  background: transparent !important;
  color: var(--text-primary) !important;
}
:deep(.p-inputtext) {
  border: 1px solid var(--card-border) !important;
}
:deep(.p-dropdown-trigger),
:deep(.p-datepicker-trigger),
:deep(.p-select-trigger),
:deep(.p-icon) {
  color: var(--text-secondary) !important;
}

:deep(.p-inputtext:focus),
:deep(.p-inputtext:focus-visible),
:deep(.p-inputwrapper-focus .p-inputtext),
:deep(.p-dropdown.p-focus),
:deep(.p-calendar.p-focus),
:deep(.p-calendar:focus-within),
:deep(.p-focus) {
  outline: none !important;
  box-shadow: none !important;
  border-color: var(--card-border) !important;
}

:deep(.p-datatable) .p-datatable-thead > tr > th {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff;
  font-weight: 700;
  padding: 16px;
  border: none;
  text-transform: uppercase;
  font-size: 0.875rem;
  letter-spacing: 0.05em;
}
:deep(.p-datatable) .p-datatable-tbody > tr {
  transition: all 0.2s ease;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-bg);
}
:deep(.p-datatable) .p-datatable-tbody > tr:nth-child(even) {
  background: rgba(130, 42, 204, 0.03);
}
.dark :deep(.p-datatable) .p-datatable-tbody > tr:nth-child(even) {
  background: rgba(130, 42, 204, 0.08);
}
:deep(.p-datatable) .p-datatable-tbody > tr:hover {
  background: var(--card-hover-bg) !important;
}
:deep(.p-datatable) .p-datatable-tbody > tr > td {
  padding: 16px;
  border: none;
  color: var(--text-primary);
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded {
  width: 2.5rem;
  height: 2.5rem;
  transition: all 0.2s ease;
  border: 2px solid currentColor;
  border-radius: 50%;
  opacity: 0.6;
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded:hover {
  transform: scale(1.1);
  opacity: 1;
  box-shadow: 0 2px 8px currentColor;
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded.text-\[var\(--primary\)\] {
  border-color: rgba(130, 42, 204, 0.4);
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded.text-\[var\(--primary\)\]:hover {
  border-color: rgba(130, 42, 204, 0.8);
  box-shadow: 0 2px 8px rgba(130, 42, 204, 0.3);
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded.text-emerald-500 {
  border-color: rgba(16, 185, 129, 0.4);
}
:deep(.p-datatable) .p-button.p-button-text.p-button-rounded.text-emerald-500:hover {
  border-color: rgba(16, 185, 129, 0.8);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

:deep(.p-datatable) .preview-row {
  background: rgba(245, 158, 11, 0.08) !important;
  box-shadow: inset 3px 0 0 rgba(245, 158, 11, 0.45);
}
:deep(.p-datatable) .preview-row:nth-child(even) {
  background: rgba(245, 158, 11, 0.15) !important;
}
:deep(.p-datatable) .preview-row:hover {
  background: rgba(245, 158, 11, 0.20) !important;
}
@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  :deep(.p-datatable) .preview-row {
    background: color-mix(in oklab, #f59e0b 12%, var(--card-bg)) !important;
  }
  :deep(.p-datatable) .preview-row:nth-child(even) {
    background: color-mix(in oklab, #f59e0b 18%, var(--card-bg)) !important;
  }
  :deep(.p-datatable) .preview-row:hover {
    background: color-mix(in oklab, #f59e0b 24%, var(--card-bg)) !important;
  }
}
.dark :deep(.p-datatable) .preview-row {
  background: rgba(245, 158, 11, 0.15) !important;
  box-shadow: inset 3px 0 0 rgba(245, 158, 11, 0.6);
}
.dark :deep(.p-datatable) .preview-row:nth-child(even) {
  background: rgba(245, 158, 11, 0.22) !important;
}
.dark :deep(.p-datatable) .preview-row:hover {
  background: rgba(245, 158, 11, 0.28) !important;
}

:deep(.p-button.btn-primary) {
  background: transparent !important;
  border: 2px solid var(--primary) !important;
  color: var(--primary) !important;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(130, 42, 204, 0.15);
}
:deep(.p-button.btn-primary:hover) {
  background: rgba(130, 42, 204, 0.1) !important;
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.btn-primary:active) {
  transform: translateY(0);
}

:deep(.p-button.p-button-outlined) {
  font-weight: 600;
  transition: all 0.2s ease;
}
:deep(.p-button.p-button-outlined.border-amber-500) {
  border-color: rgb(245 158 11) !important;
  color: rgb(217 119 6) !important;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
}
:deep(.p-button.p-button-outlined.border-amber-500:hover) {
  background: rgba(245, 158, 11, 0.1) !important;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.p-button-outlined.border-cyan-500) {
  border-color: rgb(6 182 212) !important;
  color: rgb(8 145 178) !important;
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.15);
}
:deep(.p-button.p-button-outlined.border-cyan-500:hover) {
  background: rgba(6, 182, 212, 0.1) !important;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.p-button-outlined.border-emerald-500) {
  border-color: rgb(16 185 129) !important;
  color: rgb(5 150 105) !important;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
}
:deep(.p-button.p-button-outlined.border-emerald-500:hover) {
  background: rgba(16, 185, 129, 0.1) !important;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25) !important;
  transform: translateY(-1px);
}

.csv-action-btn {
  display: none;
}

@media (min-width: 768px) {
  .csv-action-btn {
    display: inline-flex;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.fab-enter-active,
.fab-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fab-enter-from,
.fab-leave-to {
  opacity: 0;
  transform: scale(0.5) rotate(180deg);
}

.overlay-enter-active,
.overlay-leave-active {
  transition: all 0.3s ease;
}
.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
  backdrop-filter: blur(0);
}

.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.menu-slide-enter-from,
.menu-slide-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

.p-dropdown-panel, .p-select-panel, .p-datepicker {
  background: var(--card-bg) !important;
  border: 1px solid var(--card-border) !important;
  box-shadow: 0 4px 16px var(--shadow-md) !important;
}
.p-dropdown-items .p-dropdown-item,
.p-select-list .p-select-option {
  color: var(--text-primary) !important;
}
.p-dropdown-items .p-dropdown-item:hover,
.p-select-list .p-select-option:hover,
.p-select-list .p-select-option.p-focus {
  background: var(--card-hover-bg) !important;
  color: var(--text-primary) !important;
}
.p-dropdown-items .p-dropdown-item.p-highlight,
.p-select-list .p-select-option.p-selected,
.p-select-list .p-select-option.p-highlight,
.p-yearpicker .p-yearpicker-year.p-highlight {
  background: linear-gradient(135deg, var(--primary), var(--primary-2)) !important;
  color: #fff !important;
}
.p-datepicker .p-datepicker-header {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
  border-bottom: 1px solid var(--card-border) !important;
}
.p-datepicker .p-yearpicker .p-yearpicker-year:hover,
.p-monthpicker .p-monthpicker-month:hover,
.p-datepicker-header button:hover {
  background: var(--card-hover-bg) !important;
}

.preview-confirm-summary {
  background: var(--bg-tertiary);
  border-color: var(--card-border);
  color: var(--text-primary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  .preview-confirm-summary {
    background: linear-gradient(
      145deg,
      color-mix(in oklab, var(--bg-tertiary) 92%, #ffffff 8%),
      color-mix(in oklab, var(--bg-tertiary) 84%, var(--primary) 16%)
    );
    border-color: color-mix(in oklab, var(--card-border) 65%, var(--primary) 35%);
  }
}

.preview-confirm-summary-row {
  border-color: var(--border-color);
}

.preview-confirm-help-text {
  color: var(--text-secondary);
}

.dark .preview-confirm-summary {
  background: #1f2937;
  border-color: #374151;
  color: #e5e7eb;
}

.dark .preview-confirm-summary-row {
  border-color: #374151;
}

.dark .preview-confirm-help-text {
  color: #9ca3af;
}

:global(.preview-confirm-root) {
  background: var(--card-bg) !important;
  border: 1px solid var(--card-border) !important;
  color: var(--text-primary) !important;
  overflow: hidden;
  box-shadow: 0 18px 42px var(--shadow-lg), 0 8px 22px var(--shadow-purple) !important;
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  :global(.preview-confirm-root) {
    background: linear-gradient(
      165deg,
      color-mix(in oklab, var(--card-bg) 93%, #ffffff 7%),
      color-mix(in oklab, var(--card-bg) 86%, var(--primary) 14%)
    ) !important;
    border-color: color-mix(in oklab, var(--card-border) 55%, var(--primary) 45%) !important;
  }
}

:global(.preview-confirm-mask) {
  backdrop-filter: blur(2px);
  background: rgba(15, 23, 42, 0.32) !important;
}

:global(.preview-confirm-header),
:global(.preview-confirm-content),
:global(.preview-confirm-footer) {
  color: var(--text-primary) !important;
}

:global(.preview-confirm-header) {
  background: var(--bg-tertiary) !important;
  border-bottom: 1px solid var(--card-border) !important;
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  :global(.preview-confirm-header) {
    background: linear-gradient(
      180deg,
      color-mix(in oklab, var(--bg-tertiary) 90%, #ffffff 10%),
      color-mix(in oklab, var(--bg-tertiary) 84%, var(--primary) 16%)
    ) !important;
  }
}

:global(.preview-confirm-content),
:global(.preview-confirm-footer) {
  background: transparent !important;
}

:global(.preview-confirm-title),
:global(.preview-confirm-content p),
:global(.preview-confirm-content label) {
  color: var(--text-primary) !important;
}

:global(.preview-confirm-close-btn) {
  color: var(--text-secondary) !important;
  border: 1px solid var(--card-border) !important;
  background: var(--bg-secondary) !important;
  transition: all 0.2s ease;
}

:global(.preview-confirm-close-btn:hover) {
  color: var(--primary) !important;
  border-color: var(--primary) !important;
  background: var(--card-hover-bg) !important;
}

:deep(.preview-confirm-field .p-inputtext),
:deep(.preview-confirm-field .p-inputnumber-input),
:deep(.preview-confirm-field .p-datepicker-input) {
  background: var(--bg-secondary) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border-color) !important;
}

:deep(.preview-confirm-field .p-inputtext::placeholder),
:deep(.preview-confirm-field .p-inputnumber-input::placeholder) {
  color: var(--text-tertiary) !important;
}

:deep(.preview-confirm-field.p-inputwrapper-focus .p-inputtext),
:deep(.preview-confirm-field .p-inputtext:focus),
:deep(.preview-confirm-field .p-inputnumber-input:focus) {
  border-color: var(--primary) !important;
  box-shadow: 0 0 0 0.16rem color-mix(in oklab, var(--primary) 24%, transparent) !important;
}

.dark :deep(.preview-confirm-field .p-inputtext),
.dark :deep(.preview-confirm-field .p-inputnumber-input),
.dark :deep(.preview-confirm-field .p-datepicker-input) {
  background: #111827 !important;
  color: #f3f4f6 !important;
  border-color: #4b5563 !important;
}

.dark :deep(.preview-confirm-field .p-inputtext::placeholder),
.dark :deep(.preview-confirm-field .p-inputnumber-input::placeholder) {
  color: #9ca3af !important;
}

:global(.dark .preview-confirm-root) {
  background: #111827 !important;
  border-color: #374151 !important;
  color: #f3f4f6 !important;
}

:global(.dark .preview-confirm-header),
:global(.dark .preview-confirm-content),
:global(.dark .preview-confirm-footer) {
  background: #111827 !important;
  color: #f3f4f6 !important;
}

:global(.dark .preview-confirm-header) {
  border-bottom-color: #374151 !important;
}

:global(.dark .preview-confirm-title),
:global(.dark .preview-confirm-content p),
:global(.dark .preview-confirm-content label) {
  color: #f3f4f6 !important;
}

:global(.dark .preview-confirm-close-btn) {
  color: #9ca3af !important;
}

:global(.dark .preview-confirm-close-btn:hover) {
  background: #1f2937 !important;
  color: #f3f4f6 !important;
}

:global(.dark .preview-confirm-mask) {
  background: rgba(2, 6, 23, 0.58) !important;
}
</style>
