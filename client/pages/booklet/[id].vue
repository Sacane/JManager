<script setup lang="ts">
import type { AxiosError } from 'axios'
import type { AppTableColumn } from '~/components/AppTable.vue'
import { useConfirm } from 'primevue/useconfirm'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import { capitalizeFirst, getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: ['auth'],
})

const { findBalancesByIdMonthAndYear, findTransactionsByIdMonthAndYear, findByIdMonthAndYear, regenerateDeletedPrevisionalTransactions } = useBooklet()
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
const isSmallDesktop = ref(false)
const isSidebarMode = ref(false)
const csvImportDialogRef = ref<any>(null)
const isMobileMenuOpen = ref(false)
const transactionFilter = ref<'all' | 'preview' | 'confirmed'>('all')
const globalFilter = ref<'none' | 'all' | 'preview'>('none')
const allTransactions = ref<DisplayTransaction[]>([])
const selectedTagFilter = ref<string>('')
const isConfirmPreviewDialogVisible = ref(false)
const newAmountForPreview = ref<number | null>(null)
const newDateForPreview = ref<Date | null>(null)
const transactionToConfirm = ref<DisplayTransaction | null>(null)
const hasRegenerableTransactions = ref(false)

const MOBILE_PAGE_SIZE = 200

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
const transactionsCount = computed(() => {
  const source = globalFilter.value !== 'none' ? allTransactions.value : actualTransactions.value
  return source.length
})
const subTagsByParent = computed(() => {
  const map: Record<string, TagDTO[]> = {}
  for (const t of tags.value) {
    if (t.parentId) {
      if (!map[t.parentId]) map[t.parentId] = []
      map[t.parentId]!.push(t)
    }
  }
  return map
})
const selectedParentTagFilter = ref<string>('')
const selectedSubTagFilter = ref<string>('')
watch(selectedTagFilter, (val) => {
  if (val) selectedSubTagFilter.value = ''
})
watch(selectedSubTagFilter, (val) => {
  if (val) selectedTagFilter.value = ''
})
const transactionTagIds = computed(() => {
  const ids = new Set<string>()
  for (const t of actualTransactions.value) {
    if (t.tagDTO?.tagId) ids.add(t.tagDTO.tagId)
  }
  return ids
})
const usedParentTagIds = computed(() => {
  const ids = new Set<string>()
  for (const tagId of transactionTagIds.value) {
    const found = tags.value.find(t => t.tagId === tagId)
    if (found?.parentId) {
      ids.add(found.parentId)
    } else if (found && !found.parentId) {
      ids.add(tagId)
    }
  }
  return ids
})
const tagFilterOptions = computed(() => [
  { label: 'Tous les tags', value: '', colorDTO: null as null | { red: number, green: number, blue: number } },
  ...tags.value
    .filter(t => !t.parentId && usedParentTagIds.value.has(t.tagId ?? ''))
    .map(t => ({ label: t.label, value: t.tagId ?? '', colorDTO: t.colorDTO })),
])
const subTagFilterOptions = computed(() => [
  { label: 'Tous les sous-tags', value: '', colorDTO: null as null | { red: number, green: number, blue: number } },
  ...tags.value
    .filter(t => !!t.parentId && transactionTagIds.value.has(t.tagId ?? ''))
    .map(t => ({ label: t.label, value: t.tagId ?? '', colorDTO: t.colorDTO })),
])
const monthOptions = computed(() => useDate().months.map((m: string) => translate(m)))
const previewTransactionsCount = computed(() => {
  const source = globalFilter.value !== 'none' ? allTransactions.value : actualTransactions.value
  return source.filter(t => t.isPreview).length
})
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
const loadGlobalScope = LOADING_SCOPES.bookletDetails.loadGlobal
const bookTransactionScope = LOADING_SCOPES.bookletDetails.createTransaction
const editTransactionScope = LOADING_SCOPES.bookletDetails.editTransaction
const fetchTransactionScope = LOADING_SCOPES.bookletDetails.fetchTransaction
const deleteTransactionScope = LOADING_SCOPES.bookletDetails.deleteTransaction
const confirmPreviewScope = LOADING_SCOPES.bookletDetails.confirmPreview
const exportCsvScope = LOADING_SCOPES.bookletDetails.exportCsv
const regenerateScope = LOADING_SCOPES.bookletDetails.regenerate
const isBookletLoading = computed(() => isScopeLoading(loadBookletScope))
const isGlobalFilterLoading = computed(() => isScopeLoading(loadGlobalScope))
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
  let result = globalFilter.value !== 'none' ? allTransactions.value : actualTransactions.value

  if (globalFilter.value === 'preview') {
    result = result.filter(t => t.isPreview)
  } else {
    if (transactionFilter.value === 'preview') {
      result = result.filter(t => t.isPreview)
    } else if (transactionFilter.value === 'confirmed') {
      result = result.filter(t => !t.isPreview)
    }
  }

  if (selectedParentTagFilter.value !== '') {
    const childIds = (subTagsByParent.value[selectedParentTagFilter.value] ?? []).map(c => c.tagId)
    const allowedIds = new Set([selectedParentTagFilter.value, ...childIds])
    result = result.filter(t => allowedIds.has(t.tagDTO?.tagId ?? ''))
  }
  if (selectedTagFilter.value !== '') {
    const childIds = (subTagsByParent.value[selectedTagFilter.value] ?? []).map(c => c.tagId)
    const allowedIds = new Set([selectedTagFilter.value, ...childIds])
    result = result.filter(t => allowedIds.has(t.tagDTO?.tagId ?? ''))
  }
  if (selectedSubTagFilter.value !== '') {
    result = result.filter(t => (t.tagDTO?.tagId ?? '') === selectedSubTagFilter.value)
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
        findTransactionsByIdMonthAndYear(bookletId, month, bookletData.year, {}, isMobile.value ? 0 : currentPage.value, isMobile.value ? MOBILE_PAGE_SIZE : pageSize.value),
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

function exitGlobalMode() {
  globalFilter.value = 'none'
  allTransactions.value = []
}

async function loadGlobalTransactions() {
  await withLoading(async () => {
    try {
      const bookletId = (route.params as any)?.id as string
      const month = numberFromMonth(bookletData.month) as number
      const report = await findByIdMonthAndYear(bookletId, month, bookletData.year)
      allTransactions.value = report.transactions
        .map((transaction, index) => asDisplayableTransaction(transaction, index))
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    } catch (err) {
      toast.errorAxios(err as AxiosError)
      globalFilter.value = 'none'
    }
  }, loadGlobalScope)
}

async function onGlobalFilterChange(newFilter: 'none' | 'all' | 'preview') {
  if (newFilter === 'none') {
    exitGlobalMode()
    selectedTransactions.value = []
    return
  }
  const wasAlreadyGlobal = globalFilter.value !== 'none'
  globalFilter.value = newFilter
  selectedTransactions.value = []
  if (!wasAlreadyGlobal || allTransactions.value.length === 0) {
    await loadGlobalTransactions()
  }
}

async function retrieveTags() {
  try {
    tags.value = await tag.getAllTags()
  } catch (err) {
    toast.errorAxios(err as AxiosError)
  }
}

function onMonthChange() {
  exitGlobalMode()
  currentPage.value = 0
  loadBookletData()
}

function onYearChange() {
  exitGlobalMode()
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

      exitGlobalMode()
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

      exitGlobalMode()
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

      exitGlobalMode()
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

      exitGlobalMode()
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
      exitGlobalMode()
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

function resolveDisplayTag(tagDTO: TagDTO | null): TagDTO | null {
  if (!tagDTO) return null
  if (!tagDTO.parentId) return tagDTO
  return tags.value.find(t => t.tagId === tagDTO.parentId) ?? tagDTO
}

function getParentTag(tagDTO: TagDTO): TagDTO {
  return resolveDisplayTag(tagDTO) ?? tagDTO
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
  const width = window.innerWidth
  isMobile.value = width <= 768
  isSmallDesktop.value = width > 768 && width <= 1540
  isSidebarMode.value = !isMobile.value && (window.innerHeight < 768 || isSmallDesktop.value)
}

const FR_MONTHS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']

function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function dayGroupLabel(dateKey: string): string {
  const now = new Date()
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (dateKey === toDateKey(now)) return "Aujourd'hui"
  if (dateKey === toDateKey(yesterday)) return 'Hier'
  const [, month, day] = dateKey.split('-').map(Number)
  return `${day} ${FR_MONTHS[(month - 1)]}`
}

const transactionsByDay = computed(() => {
  const map = new Map<string, DisplayTransaction[]>()
  for (const t of filteredTransactions.value) {
    const key = toDateKey(new Date(t.date))
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(t)
  }
  return [...map.entries()]
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([key, transactions]) => ({ dateKey: key, label: dayGroupLabel(key), transactions }))
})

const bookletTransactionColumns = computed<AppTableColumn[]>(() => [
  { selectionMode: 'multiple', style: { width: '3rem' } },
  { field: 'date', header: 'Date', sortable: true, style: { width: '90px', minWidth: '90px', maxWidth: '90px', textAlign: 'center' }, headerClass: 'col-header-center', slotName: 'date' },
  {
    field: 'label',
    header: 'Libellé',
    sortable: true,
    style: isSmallDesktop.value
      ? { width: '150px', minWidth: '150px', maxWidth: '150px', textAlign: 'center' }
      : { minWidth: '300px', textAlign: 'center' },
    headerClass: 'col-header-center',
    slotName: 'label',
  },
  { field: 'expenseSortValue', header: 'Dépenses', sortable: true, style: { minWidth: '120px', textAlign: 'center' }, slotName: 'expenses', headerClass: 'col-header-center' },
  { field: 'incomeSortValue', header: 'Recettes', sortable: true, style: { minWidth: '120px', textAlign: 'center' }, slotName: 'income', headerClass: 'col-header-center' },
  { style: { width: '150px', minWidth: '150px', maxWidth: '150px', textAlign: 'center' }, slotName: 'tag', headerSlotName: 'tagFilter' },
  { style: { width: '150px', minWidth: '150px', maxWidth: '150px' }, slotName: 'subTag', headerSlotName: 'subTagFilter' },
  { header: 'Actions', style: { width: '140px', textAlign: 'center' }, slotName: 'actions', headerClass: 'col-header-center' },
])

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
  exitGlobalMode()
  loadBookletData()
  toast.success(`${result.successCount} transactions importées avec succès !`)
}

onMounted(async () => {
  bookletData.month = monthFromNumber(new Date().getMonth() + 1) as string
  checkMobile()
  await loadBookletData()
  await retrieveTags()
  currentTransaction.tagDTO = await tag.getDefaultTag()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <ConfirmDialog />

  <div class="flex flex-col bg-gradient-to-br from-[var(--bg-gradient-from)] to-[var(--bg-gradient-to)] md:(h-full overflow-hidden)">
    <div class="flex flex-col w-full max-w-7xl mx-auto px-5 md:px-6 lg:px-8 py-3 md:(py-2 flex-1 min-h-0)">
      <BookletPageHeader
        :display-label="displayLabel"
        :transactions-count="transactionsCount"
        :preview-transactions-count="previewTransactionsCount"
        :real-sold="bookletData.realSold"
        :preview-sold="bookletData.previewSold"
        :is-mobile="isMobile"
        :selected-month="displayMonth"
        :month-options="monthOptions"
        :date-year="bookletData.dateYear"
        @update:selected-month="displayMonth = $event"
        @month-change="onMonthChange"
        @update:date-year="bookletData.dateYear = $event"
        @year-change="onYearChange"
        @back="navigateTo('/booklet')"
      />

      <BookletFilterActionBar
        :is-mobile="isMobile"
        :hide-action-buttons="isSidebarMode"
        :transaction-filter="transactionFilter"
        :global-filter="globalFilter"
        :transactions-count="transactionsCount"
        :preview-transactions-count="previewTransactionsCount"
        :is-global-filter-loading="isGlobalFilterLoading"
        :has-selection="hasSelection"
        :selected-count="selectedTransactions.length"
        :selected-amount="selectedTransactionsAmount"
        :selected-amount-label="selectedTransactionsAmountLabel"
        :has-regenerable-transactions="hasRegenerableTransactions"
        :is-any-action-loading="isAnyActionLoading"
        :is-delete-loading="isDeleteTransactionLoading"
        :is-export-csv-loading="isExportCsvLoading"
        :is-regenerate-loading="isRegenerateLoading"
        :tag-filter-options="tagFilterOptions.map(o => ({ label: o.label, value: o.value }))"
        :sub-tag-filter-options="subTagFilterOptions.map(o => ({ label: o.label, value: o.value }))"
        :selected-tag-filter="selectedTagFilter"
        :selected-sub-tag-filter="selectedSubTagFilter"
        @update:transaction-filter="transactionFilter = $event"
        @update:global-filter="onGlobalFilterChange($event)"
        @update:selected-tag-filter="selectedTagFilter = $event"
        @update:selected-sub-tag-filter="selectedSubTagFilter = $event"
        @new-transaction="openCreationDialog"
        @new-preview="openPreviewCreationDialog"
        @import-csv="openCsvImportDialog"
        @export-csv="openCsvExportDialog"
        @regenerate="regenerate"
        @delete="confirmDeleteButton"
      />

      <div v-if="!isMobile" class="flex-1 min-h-0 flex gap-3" :class="isSidebarMode ? 'flex-row' : 'flex-col'">
        <div class="flex-1 min-h-0 flex flex-col bg-[var(--card-bg)] rounded-2xl overflow-hidden border border-[var(--card-border)] shadow-lg">
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
              <span class="block text-center font-semibold text-[var(--text-secondary)]">
                {{ new Date(data.date).getDate() }}
              </span>
            </template>

            <template #body-label="{ data }">
              <div class="flex items-center justify-center gap-2 w-full overflow-hidden">
                <span
                  v-tooltip.top="data.label"
                  class="font-semibold text-[var(--text-primary)] min-w-0 truncate"
                >{{ data.label }}</span>
                <i v-if="data.isPreview" class="pi pi-clock text-amber-500 text-sm shrink-0" title="Transaction prévisionnelle" />
              </div>
            </template>

            <template #body-expenses="{ data }">
              <span v-if="!data.isIncome" class="font-extrabold text-[#FF084B]">{{ data.expensesRepresentation }}</span>
              <span v-else class="text-[var(--text-muted)] font-semibold">-</span>
            </template>

            <template #body-income="{ data }">
              <span v-if="data.isIncome" class="font-extrabold text-[#009CFE]">{{ data.incomeRepresentation }}</span>
              <span v-else class="text-[#009CFE] font-semibold">-</span>
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
                      :style="getTagStyle(option.colorDTO ?? { red: 150, green: 150, blue: 150 })"
                      class="text-xs"
                    />
                  </template>
                </Select>
              </div>
            </template>

            <template #header-subTagFilter>
              <div class="w-full" @click.stop>
                <Select
                  v-model="selectedSubTagFilter"
                  :options="subTagFilterOptions"
                  option-label="label"
                  option-value="value"
                  class="w-full text-xs"
                  size="small"
                >
                  <template #value="{ value: val }">
                    <span class="text-xs font-semibold" :class="val ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]'">
                      {{ val ? (subTagFilterOptions.find(o => o.value === val)?.label ?? val) : 'Sous-tag' }}
                    </span>
                  </template>
                  <template #option="{ option }">
                    <span v-if="!option.value" class="text-sm text-[var(--text-secondary)]">Tous les sous-tags</span>
                    <Tag
                      v-else
                      :value="option.label"
                      :style="getTagStyle(option.colorDTO ?? { red: 150, green: 150, blue: 150 })"
                      class="text-xs"
                    />
                  </template>
                </Select>
              </div>
            </template>

            <template #body-tag="{ data }">
              <div class="max-w-[130px] overflow-hidden">
                <Tag
                  :value="getParentTag(data.tagDTO).label"
                  :style="getTagStyle(getParentTag(data.tagDTO).colorDTO)"
                  class="block max-w-full truncate"
                  :title="getParentTag(data.tagDTO).label"
                />
              </div>
            </template>

            <template #body-subTag="{ data }">
              <div v-if="data.tagDTO?.parentId" class="max-w-[130px] overflow-hidden">
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
                  class="text-[#A30053] hover:bg-[#A30053]/15"
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
                  class="text-[#A30053] hover:bg-[#A30053]/15"
                  icon="pi pi-check"
                  text
                  rounded
                  size="small"
                  :loading="isConfirmPreviewLoading"
                  :disabled="isAnyActionLoading"
                  title="Valider la transaction prévisionnel"
                  @click="onConfirmPreview(data)"
                />
              </div>
            </template>
          </AppTable>
          <div v-if="globalFilter === 'none'" class="shrink-0 flex items-center justify-between flex-wrap gap-2 px-3 py-2 border-t border-[var(--card-border)]">
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
          <div v-else class="shrink-0 flex items-center gap-2 px-3 py-2 border-t border-[var(--card-border)]">
            <i class="pi pi-globe text-[var(--primary)] text-xs" />
            <span class="text-xs font-medium text-[var(--text-secondary)]">
              {{ globalFilter === 'preview' ? 'Toutes les transactions prévisionnelles du mois' : 'Toutes les transactions du mois' }}
              — <button class="text-[var(--primary)] hover:underline" @click="onGlobalFilterChange('none')">Retour à la pagination</button>
            </span>
          </div>
        </div>

        <!-- Sidebar action buttons (right side, small desktop only, height < 768px) -->
        <div
          v-if="isSidebarMode"
          class="flex flex-col gap-1.5 py-3 px-2 rounded-xl bg-[var(--card-bg)] border border-[var(--card-border)] shadow-md items-center self-start shrink-0"
        >
          <BookletActionButtons
            orientation="vertical"
            :has-selection="hasSelection"
            :selected-count="selectedTransactions.length"
            :selected-amount="selectedTransactionsAmount"
            :selected-amount-label="selectedTransactionsAmountLabel"
            :has-regenerable-transactions="hasRegenerableTransactions"
            :is-any-action-loading="isAnyActionLoading"
            :is-delete-loading="isDeleteTransactionLoading"
            :is-export-csv-loading="isExportCsvLoading"
            :is-regenerate-loading="isRegenerateLoading"
            @new-transaction="openCreationDialog"
            @new-preview="openPreviewCreationDialog"
            @import-csv="openCsvImportDialog"
            @export-csv="openCsvExportDialog"
            @regenerate="regenerate"
            @delete="confirmDeleteButton"
          />
          <div class="w-full h-px bg-[var(--card-border)] my-1" />
          <button
            v-tooltip.right="{ value: `Tout (${transactionsCount})`, pt: { text: { style: 'white-space: nowrap' } } }"
            class="w-10 h-10 flex items-center justify-center rounded-lg text-sm transition-all border"
            :class="transactionFilter === 'all' && globalFilter === 'none'
              ? 'border-[var(--primary)] text-[var(--primary)] bg-[rgba(101,8,204,0.07)]'
              : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-[var(--primary)] hover:text-[var(--primary)]'"
            @click="transactionFilter = 'all'"
          >
            <i class="pi pi-list" />
          </button>
          <button
            v-tooltip.right="{ value: `Confirmées (${transactionsCount - previewTransactionsCount})`, pt: { text: { style: 'white-space: nowrap' } } }"
            class="w-10 h-10 flex items-center justify-center rounded-lg text-sm transition-all border"
            :class="transactionFilter === 'confirmed' && globalFilter === 'none'
              ? 'border-emerald-500 text-emerald-600 bg-emerald-500/8'
              : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-emerald-500 hover:text-emerald-600'"
            @click="transactionFilter = 'confirmed'"
          >
            <i class="pi pi-check-circle" />
          </button>
          <button
            v-tooltip.right="`Prévisionnelles (${previewTransactionsCount})`"
            class="w-10 h-10 flex items-center justify-center rounded-lg text-sm transition-all border"
            :class="transactionFilter === 'preview' && globalFilter === 'none'
              ? 'border-amber-500 text-amber-600 bg-amber-500/8'
              : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-amber-500 hover:text-amber-600'"
            @click="transactionFilter = 'preview'"
          >
            <i class="pi pi-clock" />
          </button>
          <div class="w-full h-px bg-[var(--card-border)] my-1" />
          <button
            v-tooltip.right="{ value: 'Tout le mois', pt: { text: { style: 'white-space: nowrap' } } }"
            class="w-10 h-10 flex items-center justify-center rounded-lg text-sm transition-all border"
            :class="globalFilter === 'all'
              ? 'border-[var(--primary)] text-[var(--primary)] bg-[rgba(101,8,204,0.07)]'
              : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-[var(--primary)] hover:text-[var(--primary)]'"
            :disabled="isGlobalFilterLoading"
            @click="onGlobalFilterChange(globalFilter === 'all' ? 'none' : 'all')"
          >
            <i :class="isGlobalFilterLoading && globalFilter === 'all' ? 'pi pi-spin pi-spinner' : 'pi pi-globe'" />
          </button>
          <button
            v-tooltip.right="{ value: 'Prév. du mois', pt: { text: { style: 'white-space: nowrap' } } }"
            class="w-10 h-10 flex items-center justify-center rounded-lg text-sm transition-all border"
            :class="globalFilter === 'preview'
              ? 'border-amber-500 text-amber-600 bg-amber-500/8'
              : 'border-[var(--card-border)] text-[var(--text-secondary)] hover:border-amber-500 hover:text-amber-600'"
            :disabled="isGlobalFilterLoading"
            @click="onGlobalFilterChange(globalFilter === 'preview' ? 'none' : 'preview')"
          >
            <i :class="isGlobalFilterLoading && globalFilter === 'preview' ? 'pi pi-spin pi-spinner' : 'pi pi-calendar'" />
          </button>
        </div>
      </div>

      <div v-else class="flex flex-col pb-20">
        <!-- Warning: too many transactions even with large page size -->
        <div v-if="totalPages > 1" class="flex items-center gap-2 px-3 py-2 mb-2 rounded-xl bg-amber-500/10 border border-amber-500/20">
          <i class="pi pi-info-circle text-amber-600 text-xs shrink-0" />
          <span class="text-xs font-medium text-amber-700 dark:text-amber-400">Volume élevé — utilisez les filtres pour affiner les résultats</span>
        </div>

        <!-- Empty state -->
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

        <!-- Banking-style grouped by day -->
        <template v-else>
          <template v-for="group in transactionsByDay" :key="group.dateKey">
            <!-- Day header -->
            <div class="px-1 pt-4 pb-1.5 first:pt-1">
              <span class="text-xs font-semibold text-[var(--text-tertiary)] tracking-wide">
                {{ group.label }}
              </span>
            </div>

            <!-- Transactions card for this day -->
            <div class="bg-[var(--card-bg)] rounded-xl overflow-hidden border border-[var(--card-border)] shadow-sm">
              <template
                v-for="(transaction, tIndex) in group.transactions"
                :key="transaction.selectionKey || transaction.id || `t-${tIndex}`"
              >
                <div v-if="tIndex > 0" class="h-px mx-3 bg-black/10 dark:bg-white/20" />
              <div
                class="flex items-center gap-3 py-3 px-3 transition-colors active:bg-[var(--card-hover-bg)]"
                :class="isSelected(transaction) ? 'bg-[var(--primary)]/5' : transaction.isPreview ? 'bg-amber-500/5' : ''"
                @click="toggleSelection(transaction)"
              >
                <!-- Checkbox -->
                <Checkbox
                  :model-value="isSelected(transaction)"
                  :binary="true"
                  class="shrink-0"
                  @click.stop="toggleSelection(transaction)"
                />

                <!-- Label + tag -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-1.5">
                    <span class="text-sm font-semibold text-[var(--text-primary)] truncate">{{ transaction.label }}</span>
                    <i v-if="transaction.isPreview" class="pi pi-clock text-amber-500 text-xs shrink-0" />
                  </div>
                  <div class="mt-0.5">
                    <span
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold"
                      :style="getTagStyle(getParentTag(transaction.tagDTO).colorDTO)"
                    >{{ getParentTag(transaction.tagDTO).label }}</span>
                  </div>
                </div>

                <!-- Amount + actions -->
                <div class="shrink-0 flex flex-col items-end gap-1.5">
                  <span
                    class="text-base font-semibold tabular-nums"
                    :class="transaction.isIncome ? 'text-[#009CFE]' : 'text-[#FF084B]'"
                  >
                    {{ transaction.isIncome ? '+' : '-' }}{{ Number.parseFloat(transaction?.value?.toString() ?? '0').toFixed(2) }}&nbsp;€
                  </span>
                  <div class="flex items-center gap-1.5">
                    <button
                      v-if="transaction.id"
                      class="w-10 h-10 flex items-center justify-center rounded-xl bg-[#A30053]/10 border border-[#A30053]/20 text-[#A30053] hover:bg-[#A30053]/20 active:scale-95 transition-all disabled:opacity-40"
                      :disabled="isAnyActionLoading"
                      aria-label="Modifier"
                      @click.stop="onEditTransaction({ data: transaction })"
                    >
                      <i class="pi pi-pencil text-sm" />
                    </button>
                    <button
                      v-if="transaction.isPreview"
                      class="w-10 h-10 flex items-center justify-center rounded-xl bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 hover:bg-emerald-500/20 active:scale-95 transition-all disabled:opacity-40"
                      :disabled="isAnyActionLoading"
                      aria-label="Valider"
                      @click.stop="onConfirmPreview(transaction)"
                    >
                      <i class="pi pi-check text-sm" :class="isConfirmPreviewLoading ? 'pi-spin' : ''" />
                    </button>
                  </div>
                </div>
              </div>
              </template>
            </div>
          </template>
        </template>
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

      <BookletConfirmPreviewDialog
        :visible="isConfirmPreviewDialogVisible"
        :transaction="transactionToConfirm"
        :loading="isConfirmPreviewLoading"
        :new-amount="newAmountForPreview"
        :new-date="newDateForPreview"
        @update:visible="isConfirmPreviewDialogVisible = $event"
        @update:new-amount="newAmountForPreview = $event"
        @update:new-date="newDateForPreview = $event"
        @confirm="confirmPreview"
      />

      <CsvImportDialog
        ref="csvImportDialogRef"
        :booklet-id="bookletData.id"
        :month="bookletData.month"
        :year="bookletData.year"
        @import-success="onCsvImportSuccess"
      />

      <BookletCsvMobileMenu
        :is-mobile="isMobile"
        :model-value="isMobileMenuOpen"
        :has-regenerable-transactions="hasRegenerableTransactions"
        :is-any-action-loading="isAnyActionLoading"
        :is-regenerate-loading="isRegenerateLoading"
        @update:model-value="isMobileMenuOpen = $event"
        @new-transaction="openCreationDialog"
        @new-preview="openPreviewCreationDialog"
        @regenerate="regenerate"
        @import-csv="openCsvImportDialog"
        @export-csv="openCsvExportDialog"
      />
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
  box-shadow: 0 2px 8px rgba(101, 8, 204, 0.15);
}
:deep(.p-button.btn-primary:hover) {
  background: rgba(101, 8, 204, 0.1) !important;
  box-shadow: 0 4px 12px rgba(101, 8, 204, 0.25) !important;
  transform: translateY(-1px);
}
:deep(.p-button.btn-primary:active) {
  transform: translateY(0);
}

:deep(.p-button.p-button-outlined) {
  font-weight: 600;
  transition: all 0.2s ease;
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

/* Tag filter buttons */
.tag-filter-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;

  &:hover {
    border-color: var(--primary);
    color: var(--text-primary);
  }

  &.active {
    border-color: transparent;
    font-weight: 600;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
  }

  &.sub {
    font-size: 0.75rem;
    padding: 0.25rem 0.5rem;
  }
}

:deep(.p-checkbox.p-checkbox-checked .p-checkbox-box) {
  background: #FFC108 !important;
  border-color: #FFC108 !important;
}
</style>
