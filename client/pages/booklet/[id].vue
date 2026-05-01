<script setup lang="ts">
import type { AxiosError } from 'axios'
import type { AppTableColumn } from '~/components/AppTable.vue'
import { useConfirm } from 'primevue/useconfirm'
import useCsvImport from '~/composables/useCsvImport'
import useTransaction from '~/composables/useTransaction'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import authMiddleware from '~/middleware/auth'
import { capitalizeFirst, getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const { findBalancesByIdMonthAndYear, findTransactionsByIdMonthAndYear, regenerateDeletedPrevisionalTransactions } = useBooklet()
const route = useRoute()
const toast = useJToast()
const confirm = useConfirm()

const { englishMonth, translate, monthFromNumber, numberFromMonth } = useDate()
const tag = useTag()
const { deleteTransaction, confirmPreviewTransaction, confirmVirtualTransaction, saveTransaction, editTransaction, findTransactionById } = useTransaction()
const { downloadCsvExport } = useCsvImport()
const { isScopeLoading, withLoading } = useLoading()

type DisplayTransaction = TransactionResultDTO & {
  selectionKey: string
  expensesRepresentation: string
  incomeRepresentation: string
  expenseSortValue: number | null
  incomeSortValue: number | null
}

const selectedTransactions = ref<DisplayTransaction[]>([])
const actualTransactions = ref<DisplayTransaction[]>([])
const tags = ref<TagDTO[]>([])

const isCreationDialogVisible = ref(false)
const isEditDialogVisible = ref(false)
const isMobile = ref(false)
const csvImportDialogRef = ref<any>(null)
const isMobileMenuOpen = ref(false)
const transactionFilter = ref<'all' | 'preview' | 'confirmed'>('all')
const selectedTagFilter = ref<string>('')
const isConfirmPreviewDialogVisible = ref(false)
const newAmountForPreview = ref<number | null>(null)
const newDateForPreview = ref<Date | null>(null)
const transactionToConfirm = ref<DisplayTransaction | null>(null)
const hasRegenerableTransactions = ref(false)

const PAGE_SIZE_KEY_BOOKLET = 'jmanager.pagination.bookletTransactions.pageSize'
const pageSizeOptions = [5, 10, 20, 30, 50]
const pageSize = useLocalStorage(PAGE_SIZE_KEY_BOOKLET, 10)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(1)

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
const transactionsCount = computed(() => actualTransactions.value.length)
const tagFilterOptions = computed(() => [
  { label: 'Tous les tags', value: '', colorDTO: null as null | { red: number, green: number, blue: number } },
  ...tags.value.map(t => ({ label: t.label, value: t.tagId ?? '', colorDTO: t.colorDTO })),
])
const previewTransactionsCount = computed(() => actualTransactions.value.filter(t => t.isPreview).length)
const hasSelection = computed(() => selectedTransactions.value.length > 0)
const selectedTransactionsAmount = computed(() => selectedTransactions.value.reduce((total, transaction) => {
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
const loadBookletScope = LOADING_SCOPES.bookletDetails.load
const bookTransactionScope = LOADING_SCOPES.bookletDetails.createTransaction
const editTransactionScope = LOADING_SCOPES.bookletDetails.editTransaction
const fetchTransactionScope = LOADING_SCOPES.bookletDetails.fetchTransaction
const deleteTransactionScope = LOADING_SCOPES.bookletDetails.deleteTransaction
const confirmPreviewScope = LOADING_SCOPES.bookletDetails.confirmPreview
const exportCsvScope = LOADING_SCOPES.bookletDetails.exportCsv
const regenerateScope = LOADING_SCOPES.bookletDetails.regenerate
const isBookletLoading = computed(() => isScopeLoading(loadBookletScope))
const isBookTransactionLoading = computed(() => isScopeLoading(bookTransactionScope))
const isEditTransactionLoading = computed(() => isScopeLoading(editTransactionScope))
const isFetchTransactionLoading = computed(() => isScopeLoading(fetchTransactionScope))
const isDeleteTransactionLoading = computed(() => isScopeLoading(deleteTransactionScope))
const isConfirmPreviewLoading = computed(() => isScopeLoading(confirmPreviewScope))
const isExportCsvLoading = computed(() => isScopeLoading(exportCsvScope))
const isRegenerateLoading = computed(() => isScopeLoading(regenerateScope))
const isAnyActionLoading = computed(() =>
  isBookletLoading.value
  || isBookTransactionLoading.value
  || isEditTransactionLoading.value
  || isFetchTransactionLoading.value
  || isDeleteTransactionLoading.value
  || isConfirmPreviewLoading.value
  || isExportCsvLoading.value
  || isRegenerateLoading.value,
)

const filteredTransactions = computed(() => {
  let result = actualTransactions.value
  if (transactionFilter.value === 'preview') {
    result = result.filter(t => t.isPreview)
  } else if (transactionFilter.value === 'confirmed') {
    result = result.filter(t => !t.isPreview)
  }
  if (selectedTagFilter.value !== '') {
    result = result.filter(t => (t.tagDTO?.tagId ?? '') === selectedTagFilter.value)
  }
  return result
})

function asDisplayableTransaction(transaction: TransactionResultDTO, index = 0): DisplayTransaction {
  const fallbackTag: TagDTO = {
    tagId: undefined,
    label: 'Aucune',
    isDefault: true,
    colorDTO: { red: 255, green: 255, blue: 255 },
  }

  const numericValue = Number.parseFloat(transaction?.value?.toString() ?? '0')
  const tagDTO = transaction.tagDTO ?? fallbackTag
  const selectionKey = transaction.id != null
    ? `id:${transaction.id}`
    : `virtual:${index}:${transaction.date}:${transaction.label}:${numericValue}:${transaction.isIncome}:${transaction.isPreview}:${tagDTO.tagId ?? 'no-tag'}`

  return {
    ...transaction,
    id: transaction.id,
    selectionKey,
    expensesRepresentation: !transaction.isIncome ? `${numericValue.toFixed(2)} €` : '-',
    incomeRepresentation: transaction.isIncome ? `${numericValue.toFixed(2)} €` : '-',
    expenseSortValue: !transaction.isIncome ? numericValue : null,
    incomeSortValue: transaction.isIncome ? numericValue : null,
    date: transaction.date,
    tagDTO,
  }
}

function transactionSelectionKey(transaction: TransactionCreationDTO | DisplayTransaction | any): string {
  const maybeId = transaction?.id
  if (maybeId != null) return `id:${maybeId}`
  if (transaction?.selectionKey) return String(transaction.selectionKey)

  const fallbackValue = Number.parseFloat(transaction?.value?.toString() ?? '0')
  return `virtual:fallback:${transaction?.date ?? ''}:${transaction?.label ?? ''}:${fallbackValue}:${transaction?.isIncome ?? false}:${transaction?.isPreview ?? false}:${transaction?.tagDTO?.tagId ?? 'no-tag'}`
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
      const bookletId = (route.params as any)?.id as string
      const month = numberFromMonth(bookletData.month) as number

      const [balances, transactionsRes] = await Promise.all([
        findBalancesByIdMonthAndYear(bookletId, month, bookletData.year),
        findTransactionsByIdMonthAndYear(bookletId, month, bookletData.year, {}, currentPage.value, pageSize.value),
      ])

      bookletData.label = balances.label
      bookletData.id = bookletId
      bookletData.realSold = Number.parseFloat(balances.realSold)
      bookletData.previewSold = Number.parseFloat(balances.previewSold)

      const nextTransactions = transactionsRes.transactions
        .map((transaction, index) => asDisplayableTransaction(transaction, index))
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

      actualTransactions.value = nextTransactions
      const nextTransactionKeys = new Set(nextTransactions.map(transactionSelectionKey))
      selectedTransactions.value = selectedTransactions.value.filter(t => nextTransactionKeys.has(transactionSelectionKey(t)))
      hasRegenerableTransactions.value = transactionsRes.hasRegenerableTransactions
      totalElements.value = transactionsRes.totalElements
      totalPages.value = transactionsRes.totalPages
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
  currentPage.value = 0
  loadBookletData()
}

function onYearChange() {
  currentPage.value = 0
  bookletData.year = bookletData.dateYear.getFullYear()
  loadBookletData()
}

function onPageChange(event: { page: number }) {
  currentPage.value = event.page
  loadBookletData()
}

function onBookletPageSizeChange() {
  currentPage.value = 0
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
      actualTransactions.value.push(newTransaction)
      actualTransactions.value.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

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

      const index = actualTransactions.value.findIndex(item => item?.id === result.id)
      if (index !== -1) {
        actualTransactions.value[index] = asDisplayableTransaction(result)
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
      const physicalIds = selectedTransactions.value
        .filter(t => t.id != null)
        .map(t => t.id as string)

      const virtualDescriptors: VirtualTransactionDescriptor[] = []
      const skippedVirtuals: DisplayTransaction[] = []

      for (const t of selectedTransactions.value) {
        if (t.id != null) continue
        if (t.regularTransactionId) {
          const txDate = new Date(t.date)
          virtualDescriptors.push({
            regularTransactionId: t.regularTransactionId,
            month: txDate.getMonth() + 1,
            year: txDate.getFullYear(),
          })
        } else {
          skippedVirtuals.push(t)
        }
      }

      if (physicalIds.length === 0 && virtualDescriptors.length === 0) {
        toast.warn('La sélection contient uniquement des transactions virtuelles non supprimables.')
        selectedTransactions.value = []
        return
      }

      if (skippedVirtuals.length > 0) {
        toast.warn('Certaines transactions virtuelles n\'ont pas pu être supprimées (identifiant récurrent manquant).')
      }

      const res = await deleteTransaction(
        bookletData.id,
        physicalIds,
        virtualDescriptors,
      )

      const deletedPhysical = new Set(res.deletedIds)
      const excludedMonths = new Set(res.excludedVirtualTransactions ?? [])

      actualTransactions.value = actualTransactions.value.filter((t) => {
        if (t.id != null) return !deletedPhysical.has(t.id)
        if (t.regularTransactionId && excludedMonths.size > 0) {
          const txDate = new Date(t.date)
          const yearMonth = `${txDate.getFullYear()}-${String(txDate.getMonth() + 1).padStart(2, '0')}`
          return !excludedMonths.has(yearMonth)
        }
        return true
      })
      selectedTransactions.value = []

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
    message: `Êtes-vous sûr de vouloir supprimer ${selectedTransactions.value.length} transaction${selectedTransactions.value.length > 1 ? 's' : ''} ?`,
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
      const baseDate = new Date(transaction.date)
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

        const index = actualTransactions.value.findIndex(item => item?.id === result.id)
        if (index !== -1) {
          actualTransactions.value[index] = asDisplayableTransaction(result)
        }
      } else {
        const sourceMonth = numberFromMonth(bookletData.month) as number
        const sourceYear = bookletData.year
        await confirmVirtualTransaction(
          bookletData.id,
          transaction.regularTransactionId!,
          sourceMonth,
          sourceYear,
          transaction.label,
          finalAmount,
          finalDate,
          transaction.isIncome,
          transaction.tagDTO?.tagId ?? undefined,
          transaction.tagDTO?.isDefault ?? false,
        )
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

function onConfirmPreview(transaction: DisplayTransaction) {
  transactionToConfirm.value = transaction
  const parsedDate = new Date(transaction.date)
  newDateForPreview.value = Number.isNaN(parsedDate.getTime()) ? null : parsedDate
  isConfirmPreviewDialogVisible.value = true
}

async function regenerate() {
  await withLoading(async () => {
    try {
      const bookletId = (route.params as any)?.id as string
      const month = numberFromMonth(bookletData.month) as number
      const response = await regenerateDeletedPrevisionalTransactions(bookletId, month, bookletData.year)
      await loadBookletData()
      if (response.type === 'PREVISIONAL') {
        toast.success('Transactions prévisionnelles régénérées avec succès')
      } else if (response.type === 'VIRTUAL') {
        toast.success('Transactions virtuelles restaurées avec succès')
      }
    } catch (err) {
      toast.errorAxios(err as AxiosError)
    }
  }, regenerateScope)
}

function rowClass(row: DisplayTransaction): string {
  if (row.isPreview) return 'preview-row'
  return ''
}

function toggleSelection(transaction: DisplayTransaction) {
  const transactionKey = transactionSelectionKey(transaction)
  const index = selectedTransactions.value.findIndex(t => transactionSelectionKey(t) === transactionKey)
  if (index === -1) selectedTransactions.value.push(transaction)
  else selectedTransactions.value.splice(index, 1)
}

function isSelected(transaction: DisplayTransaction): boolean {
  const transactionKey = transactionSelectionKey(transaction)
  return selectedTransactions.value.some(t => transactionSelectionKey(t) === transactionKey)
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const bookletTransactionColumns: AppTableColumn[] = [
  { selectionMode: 'multiple', style: { width: '3rem' } },
  { field: 'date', header: 'Date', sortable: true, style: { minWidth: '120px' }, slotName: 'date' },
  { field: 'label', header: 'Libellé', sortable: true, style: { minWidth: '200px' }, slotName: 'label' },
  { field: 'expenseSortValue', header: 'Dépenses', sortable: true, style: { minWidth: '120px' }, slotName: 'expenses' },
  { field: 'incomeSortValue', header: 'Recettes', sortable: true, style: { minWidth: '120px' }, slotName: 'income' },
  { style: { width: '180px', minWidth: '180px', maxWidth: '180px' }, slotName: 'tag', headerSlotName: 'tagFilter' },
  { header: 'Actions', style: { width: '140px', textAlign: 'center' }, slotName: 'actions' },
]

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
  const nonPreviewTransactions = actualTransactions.value.filter(t => !t.isPreview)
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

  <div class="flex flex-col bg-gradient-to-br from-[var(--bg-gradient-from)] to-[var(--bg-gradient-to)] md:(h-full overflow-hidden)">
    <div class="flex flex-col w-full max-w-7xl mx-auto px-5 md:px-6 lg:px-8 py-5 md:(py-4 flex-1 min-h-0)">
      <div class="bg-[var(--card-bg)] rounded-2xl p-5 shadow border border-[var(--card-border)] overflow-hidden mb-5 lg:(p-4 rounded-xl) md:(p-3 rounded-lg mb-4)">
        <div class="flex flex-col md:flex-row justify-between items-center gap-4 md:gap-4">
          <div class="flex items-center gap-4 min-w-0 md:gap-3">
            <Button class="text-[var(--primary)] w-9 h-9 rounded-full grid place-items-center hover:bg-[rgba(130,42,204,0.1)]" icon="pi pi-arrow-left" text rounded @click="navigateTo('/booklet')" />
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

            <div class="flex w-full md:w-auto gap-2 items-center">
              <Select
                v-model="displayMonth"
                :options="useDate().months.map(u => translate(u))"
                placeholder="Mois"
                class="flex-1 min-w-0 w-full md:(flex-none min-w-[120px] w-auto) border-1 rounded-lg bg-transparent"
                @change="onMonthChange"
              />
              <DatePicker
                v-model="bookletData.dateYear"
                view="year"
                date-format="yy"
                class="flex-1 min-w-0 w-full md:(flex-none min-w-[220px] w-[220px]) rounded-[14px] min-h-[46px] cursor-pointer bg-transparent"
                placeholder="Année"
                :show-icon="true"
                icon-display="input"
                @date-select="onYearChange"
              />
              <template v-if="!isMobile">
                <div class="w-px h-7 bg-[var(--border-color)] mx-1 shrink-0" />
                <div class="flex items-center gap-1 shrink-0">
                  <button
                    v-tooltip.bottom="`Tout (${transactionsCount})`"
                    class="w-8 h-8 flex items-center justify-center rounded-lg text-sm transition-all border"
                    :class="transactionFilter === 'all'
                      ? 'bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white border-transparent shadow-[0_2px_8px_rgba(130,42,204,0.25)]'
                      : 'bg-transparent text-[var(--text-secondary)] border-[var(--card-border)] hover:text-[var(--primary)] hover:border-[var(--primary)]'"
                    @click="transactionFilter = 'all'"
                  >
                    <i class="pi pi-list" />
                  </button>
                  <button
                    v-tooltip.bottom="`Confirmées (${transactionsCount - previewTransactionsCount})`"
                    class="w-8 h-8 flex items-center justify-center rounded-lg text-sm transition-all border"
                    :class="transactionFilter === 'confirmed'
                      ? 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white border-transparent shadow-[0_2px_8px_rgba(16,185,129,0.25)]'
                      : 'bg-transparent text-[var(--text-secondary)] border-[var(--card-border)] hover:text-emerald-600 hover:border-emerald-500'"
                    @click="transactionFilter = 'confirmed'"
                  >
                    <i class="pi pi-check-circle" />
                  </button>
                  <button
                    v-tooltip.bottom="`Prévisionnelles (${previewTransactionsCount})`"
                    class="w-8 h-8 flex items-center justify-center rounded-lg text-sm transition-all border"
                    :class="transactionFilter === 'preview'
                      ? 'bg-gradient-to-br from-amber-500 to-amber-600 text-white border-transparent shadow-[0_2px_8px_rgba(245,158,11,0.25)]'
                      : 'bg-transparent text-[var(--text-secondary)] border-[var(--card-border)] hover:text-amber-600 hover:border-amber-500'"
                    @click="transactionFilter = 'preview'"
                  >
                    <i class="pi pi-clock" />
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- Filtres + Actions desktop -->
      <div class="flex items-center gap-2 mb-4 overflow-x-auto pb-2">
        <template v-if="isMobile">
          <span class="text-sm font-semibold text-[var(--text-secondary)] whitespace-nowrap mr-1 shrink-0">Afficher :</span>
          <button
            class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
            :class="transactionFilter === 'all'
              ? 'bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-[0_2px_8px_rgba(130,42,204,0.25)]'
              : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-[var(--primary)]'"
            @click="transactionFilter = 'all'"
          >
            <i class="pi pi-list mr-2" />
            Tout ({{ transactionsCount }})
          </button>
          <button
            class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
            :class="transactionFilter === 'confirmed'
              ? 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white shadow-[0_2px_8px_rgba(16,185,129,0.25)]'
              : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-emerald-600'"
            @click="transactionFilter = 'confirmed'"
          >
            <i class="pi pi-check-circle mr-2" />
            Confirmées ({{ transactionsCount - previewTransactionsCount }})
          </button>
          <button
            class="px-4 py-2 rounded-lg font-semibold text-sm transition-all whitespace-nowrap shrink-0"
            :class="transactionFilter === 'preview'
              ? 'bg-gradient-to-br from-amber-500 to-amber-600 text-white shadow-[0_2px_8px_rgba(245,158,11,0.25)]'
              : 'bg-[var(--card-bg)] text-[var(--text-secondary)] border border-[var(--card-border)] hover:bg-[var(--card-hover-bg)] hover:text-amber-600'"
            @click="transactionFilter = 'preview'"
          >
            <i class="pi pi-clock mr-2" />
            Prévisionnelles ({{ previewTransactionsCount }})
          </button>
        </template>

        <!-- Actions icon-only : desktop uniquement -->
        <template v-if="!isMobile">
          <div class="w-px h-7 bg-[var(--border-color)] mx-1 shrink-0" />
          <div class="flex items-center gap-1.5 shrink-0 ml-auto">
            <template v-if="hasSelection">
              <span class="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-[var(--card-bg)] border border-[var(--card-border)] text-sm font-semibold whitespace-nowrap shrink-0">
                <i class="pi pi-check-square text-[var(--primary)] text-xs" />
                <span class="text-[var(--text-secondary)]">{{ selectedTransactions.length }}</span>
                <span class="w-px h-4 bg-[var(--border-color)] inline-block" />
                <span :class="selectedTransactionsAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">{{ selectedTransactionsAmountLabel }}</span>
              </span>
              <div class="w-px h-7 bg-[var(--border-color)] mx-1 shrink-0" />
            </template>
            <Button
              v-tooltip.bottom="'Nouvelle transaction'"
              class="btn-primary !w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0"
              icon="pi pi-plus"
              :disabled="isAnyActionLoading"
              @click="openCreationDialog"
            />
            <Button
              v-tooltip.bottom="'Transaction prévisionnelle'"
              outlined
              class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-amber-500 text-amber-600 hover:bg-amber-500/10 transition-all"
              icon="pi pi-clock"
              :disabled="isAnyActionLoading"
              @click="openPreviewCreationDialog"
            />
            <Button
              v-tooltip.bottom="'Importer CSV'"
              outlined
              class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-cyan-500 text-cyan-600 hover:bg-cyan-500/10 transition-all"
              icon="pi pi-file-import"
              :disabled="isAnyActionLoading"
              @click="openCsvImportDialog"
            />
            <Button
              v-tooltip.bottom="'Exporter CSV'"
              aria-label="Exporter CSV"
              outlined
              class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-emerald-500 text-emerald-600 hover:bg-emerald-500/10 transition-all"
              icon="pi pi-file-export"
              :loading="isExportCsvLoading"
              :disabled="isAnyActionLoading"
              @click="openCsvExportDialog"
            />
            <Button
              v-if="hasRegenerableTransactions"
              v-tooltip.bottom="'Régénérer les transactions supprimées'"
              outlined
              class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-violet-500 text-violet-600 hover:bg-violet-500/10 transition-all"
              icon="pi pi-refresh"
              :loading="isRegenerateLoading"
              :disabled="isAnyActionLoading"
              @click="regenerate"
            />
            <template v-if="hasSelection">
              <Button
                v-tooltip.bottom="`Supprimer (${selectedTransactions.length})`"
                outlined
                class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-red-500 text-red-500 hover:bg-red-500/10 transition-all"
                icon="pi pi-trash"
                :loading="isDeleteTransactionLoading"
                :disabled="isAnyActionLoading"
                @click="confirmDeleteButton"
              />
            </template>
          </div>
        </template>
      </div>

      <!-- Boutons d'action mobile -->
      <div v-if="isMobile" class="flex gap-2 mb-4">
        <Button
          class="btn-primary flex-1"
          icon="pi pi-plus"
          label="Transaction"
          :disabled="isAnyActionLoading"
          @click="openCreationDialog"
        />
        <Button
          outlined
          class="flex-1 border-amber-500 text-amber-600 hover:bg-amber-500/10 font-semibold transition-all"
          icon="pi pi-clock"
          label="Prévisionnelle"
          :disabled="isAnyActionLoading"
          @click="openPreviewCreationDialog"
        />
        <Button
          v-if="hasRegenerableTransactions"
          outlined
          class="flex-1 border-violet-500 text-violet-600 hover:bg-violet-500/10 font-semibold transition-all"
          icon="pi pi-refresh"
          label="Régénérer"
          :loading="isRegenerateLoading"
          :disabled="isAnyActionLoading"
          @click="regenerate"
        />
      </div>

      <div v-if="isMobile" class="flex flex-col gap-3 mb-5 md:mb-4">
        <Transition name="fade">
          <div v-if="hasSelection" class="flex items-center gap-3">
            <div class="flex items-center justify-between gap-4 px-4 py-2.5 rounded-xl border border-[var(--card-border)] bg-[var(--card-bg)] shadow-sm flex-1">
              <div class="flex items-center gap-2">
                <i class="pi pi-check-square text-[var(--primary)] text-sm" />
                <span class="text-sm font-semibold text-[var(--text-secondary)]">{{ selectedTransactions.length }} sélectionnée{{ selectedTransactions.length > 1 ? 's' : '' }}</span>
              </div>
              <div class="w-px h-6 bg-[var(--border-color)]" />
              <span class="text-base font-extrabold" :class="selectedTransactionsAmount >= 0 ? 'text-emerald-600' : 'text-red-500'">
                {{ selectedTransactionsAmountLabel }}
              </span>
            </div>
            <Button
              v-tooltip.bottom="`Supprimer (${selectedTransactions.length})`"
              outlined
              class="!w-10 !h-10 !p-0 !flex !items-center !justify-center shrink-0 border-red-500 text-red-500 hover:bg-red-500/10 transition-all"
              icon="pi pi-trash"
              :loading="isDeleteTransactionLoading"
              :disabled="isAnyActionLoading"
              @click="confirmDeleteButton"
            />
          </div>
        </Transition>
      </div>

      <div v-if="!isMobile" class="flex-1 min-h-0 flex flex-col bg-[var(--card-bg)] rounded-2xl overflow-hidden border border-[var(--card-border)] shadow-lg">
        <AppTable
          v-model:selection="selectedTransactions"
          class="flex-1 min-h-0"
          :columns="bookletTransactionColumns"
          :rows="filteredTransactions"
          data-key="selectionKey"
          :row-class="rowClass"
          selectable
          scrollable
          scroll-height="flex"
          :loading="isBookletLoading"
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

          <template #loading>
            <div class="flex items-center justify-center gap-2 py-8 text-[var(--text-secondary)]">
              <i class="pi pi-spin pi-spinner" />
              <span>Chargement des transactions...</span>
            </div>
          </template>

          <template #body-date="{ data }">
            <div class="flex items-center gap-2 text-[var(--text-secondary)] font-medium">
              <i class="pi pi-calendar text-[var(--primary)] text-sm" />
              <span>{{ data.date }}</span>
            </div>
          </template>

          <template #body-label="{ data }">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-[var(--text-primary)]">{{ data.label }}</span>
              <i v-if="data.isPreview" class="pi pi-clock text-amber-500 text-sm" title="Transaction prévisionnelle" />
            </div>
          </template>

          <template #body-expenses="{ data }">
            <span v-if="!data.isIncome" class="font-extrabold text-red-500">{{ data.expensesRepresentation }}</span>
            <span v-else class="text-[var(--text-muted)] font-semibold">-</span>
          </template>

          <template #body-income="{ data }">
            <span v-if="data.isIncome" class="font-extrabold text-emerald-500">{{ data.incomeRepresentation }}</span>
            <span v-else class="text-[var(--text-muted)] font-semibold">-</span>
          </template>

          <template #header-tagFilter>
            <div class="w-full" @click.stop>
              <Select
                v-model="selectedTagFilter"
                :options="tagFilterOptions"
                option-label="label"
                option-value="value"
                class="w-full text-xs"
                size="small"
              >
                <template #value="{ value: val }">
                  <span class="text-xs font-semibold" :class="val ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]'">
                    {{ val ? (tagFilterOptions.find(o => o.value === val)?.label ?? val) : 'Tag' }}
                  </span>
                </template>
                <template #option="{ option }">
                  <span v-if="!option.value" class="text-sm text-[var(--text-secondary)]">Tous les tags</span>
                  <Tag
                    v-else
                    :value="option.label"
                    :style="{ ...getTagStyle(option.colorDTO ?? { red: 150, green: 150, blue: 150 }), color: 'white', textShadow: '0 1px 2px rgba(0,0,0,0.35)' }"
                    class="text-xs"
                  />
                </template>
              </Select>
            </div>
          </template>

          <template #body-tag="{ data }">
            <div class="max-w-[148px] overflow-hidden">
              <Tag
                :value="data.tagDTO.label"
                :style="getTagStyle(data.tagDTO.colorDTO)"
                class="block max-w-full truncate"
                :title="data.tagDTO.label"
              />
            </div>
          </template>

          <template #body-actions="{ data }">
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
        </AppTable>
        <div class="shrink-0 flex items-center justify-between flex-wrap gap-2 px-3 py-2 border-t border-[var(--card-border)]">
          <div class="flex items-center gap-2">
            <span class="text-xs text-[var(--text-secondary)]">Lignes par page&nbsp;:</span>
            <Select
              v-model="pageSize"
              :options="pageSizeOptions"
              class="w-20"
              size="small"
              @change="onBookletPageSizeChange"
            />
          </div>
          <Paginator
            v-if="totalPages > 1"
            :first="currentPage * pageSize"
            :rows="pageSize"
            :total-records="totalElements"
            @page="onPageChange"
          />
        </div>
      </div>

      <div v-else class="flex flex-col">
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

        <div v-else class="p-1 flex flex-col gap-3 md:(gap-4 p-0)">
          <div
            v-for="(transaction, tIndex) in filteredTransactions"
            :key="transaction.selectionKey || transaction.id || `t-${tIndex}`"
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
:global(body) {
  scrollbar-gutter: stable;
}

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
