<script setup lang="ts">
import type { AppTableColumn } from '~/components/AppTable.vue'
import useDate from '~/composables/useDate'
import authMiddleware from '~/middleware/auth'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const { fetch } = useBooklet()
const { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById, updateRegularTransaction, deleteRegularTransaction, deleteRegularTransactions, linkRegularTransactionToBooklet, unlinkRegularTransactionFromBooklet } = useRegularTransaction()
const transactions = ref<RegularTransactionDTO[]>([])

const PAGE_SIZE_KEY_RT = 'jmanager.pagination.regularTransactions.pageSize'
const pageSizeOptions = [10, 20, 30, 50]
const pageSize = useLocalStorage(PAGE_SIZE_KEY_RT, 10)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(1)
const { frequencyToString } = useDate()
const jToast = useJToast()
const booklets = ref<OnlyBookletInfo[]>([])
const selectedTransactions = ref<RegularTransactionDTO[]>([])
const isMobile = ref(false)
const isSmallScreen = ref(false)
const confirm = useConfirm()

function loadRegularTransactions(page: number = currentPage.value, size: number = pageSize.value) {
  getRegularTransaction(page, size)
    .then((res) => {
      transactions.value = res.content
      totalElements.value = res.totalElements
      totalPages.value = res.totalPages
      currentPage.value = res.pageNumber
    })
    .catch((err) => {
      console.error(err)
    })
}

onMounted(() => {
  fetch()
    .then((res: BookletDTO[]) => {
      booklets.value = res.map(booklet => ({
        id: booklet.id,
        amount: booklet.amount,
        label: booklet.label,
        currency: booklet.currency,
      }))
    })
  loadRegularTransactions()

  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const isCreationDialogVisible = ref(false)
const isCreateRegularTransactionLoading = ref(false)

function openCreationRegularTransactionDialog() {
  isCreationDialogVisible.value = true
}
function cancelCreationDialog() {
  isCreationDialogVisible.value = false
}

async function onSave(transaction: MonthlyTransactionCreationRequest) {
  if (!transaction.bookletIds?.length) {
    jToast.warn('Veuillez sélectionner au moins un compte')
    return
  }

  isCreateRegularTransactionLoading.value = true
  try {
    const regularTransaction = await saveMonthlyTransaction(transaction)
    console.warn('Transaction saved successfully', regularTransaction)
    isCreationDialogVisible.value = false
    transactions.value.push(regularTransaction)
    jToast.success('La transaction mensuel a bien été généré')
  } catch (err: any) {
    console.error(err)
    jToast.errorAxios(err)
  } finally {
    isCreateRegularTransactionLoading.value = false
  }
}

const isEditDialogVisible = ref(false)
const selectedTransaction = ref<RegularTransactionDTO | null>(null)
const loadingTransaction = ref(false)

async function handleRowDoubleClick(event: any) {
  const transactionId = event.data.id

  if (!transactionId) {
    console.error('ID de transaction manquant')
    return
  }

  loadingTransaction.value = true
  isEditDialogVisible.value = true

  try {
    selectedTransaction.value = await getRegularTransactionById(transactionId)
  } catch (error) {
    console.error('Erreur lors de la récupération de la transaction:', error)
    isEditDialogVisible.value = false
  } finally {
    loadingTransaction.value = false
  }
}

async function handleEditSave(updatedTransaction: RegularTransactionDTO) {
  try {
    const parsedStartDate = new Date(updatedTransaction.startDate)
    const monthlyDay = Number.isNaN(parsedStartDate.getTime()) ? undefined : parsedStartDate.getDate()

    const updateRequest: UpdateRegularTransactionRequest = {
      id: updatedTransaction.id,
      label: updatedTransaction.label,
      value: updatedTransaction.value,
      startDate: updatedTransaction.startDate,
      isIncome: updatedTransaction.isIncome,
      tagDTO: updatedTransaction.tagDTO,
      frequencyProperty: updatedTransaction.frequencyProperty,
      bookletIds: updatedTransaction.bookletIds ?? [],
      recurrenceRule: {
        type: updatedTransaction.regularity,
        dayOfMonth: updatedTransaction.regularity === 'MONTHLY' ? monthlyDay : undefined,
      },
    }

    const updated = await updateRegularTransaction(updateRequest)

    const index = transactions.value.findIndex(t => t.id === updated.id)
    if (index !== -1) {
      transactions.value[index] = updated
    }

    isEditDialogVisible.value = false
    jToast.success('Transaction régulière mise à jour avec succès')
  } catch (error: any) {
    console.error('Erreur lors de la mise à jour:', error)
    jToast.errorAxios(error)
  }
}

function handleDelete(transactionId: string) {
  const transaction = transactions.value.find(t => t.id === transactionId)
  const bookletCount = transaction?.bookletIds?.length ?? 0
  const deletionMessage = bookletCount > 0
    ? `Êtes-vous sûr de vouloir supprimer cette transaction régulière ? Cette action est irréversible.\n\nCette transaction est liée à ${bookletCount} livret(s). Les liens avec ces livrets seront également supprimés.`
    : 'Êtes-vous sûr de vouloir supprimer cette transaction régulière ? Cette action est irréversible.'

  confirm.require({
    message: deletionMessage,
    header: '⚠️ Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Annuler',
    acceptLabel: 'Supprimer',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Supprimer',
      severity: 'danger',
    },
    accept: async () => {
      try {
        await deleteRegularTransaction(transactionId)

        transactions.value = transactions.value.filter(t => t.id !== transactionId)
        isEditDialogVisible.value = false

        jToast.success('Transaction régulière supprimée avec succès')
      } catch (error: any) {
        console.error('Erreur lors de la suppression:', error)
        jToast.errorAxios(error)
      }
    },
    reject: () => {

    },
  })
}

function handleBulkDelete() {
  const ids = selectedTransactions.value.map(transaction => transaction.id)

  if (ids.length === 0) {
    return
  }

  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer ${ids.length} transaction(s) régulière(s) ? Cette action est irréversible.`,
    header: '⚠️ Confirmation de suppression multiple',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Annuler',
    acceptLabel: 'Supprimer',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Supprimer',
      severity: 'danger',
    },
    accept: async () => {
      try {
        await deleteRegularTransactions({ transactionIds: ids })
        transactions.value = transactions.value.filter(transaction => !ids.includes(transaction.id))
        selectedTransactions.value = []
        isEditDialogVisible.value = false
        jToast.success(`${ids.length} transaction(s) régulière(s) supprimée(s) avec succès`)
      } catch (error: any) {
        console.error('Erreur lors de la suppression multiple:', error)
        jToast.errorAxios(error)
      }
    },
  })
}

function isSelected(transaction: RegularTransactionDTO): boolean {
  return selectedTransactions.value.some(t => t.id === transaction.id)
}

const selectedTagFilter = ref<string>('')

const tagFilterOptions = computed(() => [
  { label: 'Tous les tags', value: '', colorDTO: null as null | ColorDTO },
  ...Array.from(
    new Map(
      transactions.value
        .filter(t => t.tagDTO?.tagId)
        .map(t => [t.tagDTO.tagId, t.tagDTO]),
    ).values(),
  ).map(tag => ({ label: tag.label ?? '', value: tag.tagId ?? '', colorDTO: tag.colorDTO })),
])

const filteredTransactions = computed(() => {
  if (selectedTagFilter.value === '') return transactions.value
  return transactions.value.filter(t => (t.tagDTO?.tagId ?? '') === selectedTagFilter.value)
})

function onRtPageChange(event: { page: number }) {
  currentPage.value = event.page
  loadRegularTransactions(event.page)
}

function onRtPageSizeChange() {
  currentPage.value = 0
  loadRegularTransactions(0)
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
  isSmallScreen.value = window.innerWidth > 768 && window.innerWidth <= 1200
}

const transactionsCount = computed(() => transactions.value.length)
const selectedTransactionsCount = computed(() => selectedTransactions.value.length)

const regularTransactionColumns = computed<AppTableColumn[]>(() => {
  const cols: AppTableColumn[] = [
    { selectionMode: 'multiple', headerStyle: 'width: 3rem' },
    { field: 'label', header: 'Libellé', sortable: true, style: { minWidth: '200px' }, slotName: 'label' },
    { field: 'isIncome', header: 'Type', style: { minWidth: '130px' }, slotName: 'type' },
    { field: 'value', header: 'Montant', sortable: true, style: { minWidth: '150px' }, slotName: 'montant' },
    { style: { width: '180px', minWidth: '180px', maxWidth: '180px' }, slotName: 'tag', headerSlotName: 'tagFilter' },
    { field: 'regularity', header: 'Fréquence', style: { minWidth: '130px' }, slotName: 'regularity' },
  ]

  if (!isSmallScreen.value) {
    cols.push({ field: 'startDate', header: 'Date de début', sortable: true, style: { minWidth: '140px' }, slotName: 'startDate' })
  }

  cols.push({
    header: 'Actions',
    style: { minWidth: '160px' },
    frozen: !isSmallScreen.value,
    alignFrozen: !isSmallScreen.value ? 'right' : undefined,
    slotName: 'actions',
  })

  if (isSmallScreen.value) {
    cols.push({ field: 'startDate', header: 'Date de début', sortable: true, style: { minWidth: '140px' }, slotName: 'startDate' })
  }

  return cols
})

// ── Link dialog ──────────────────────────────────────────────────────────────
const isLinkDialogVisible = ref(false)
const linkTargetTransaction = ref<RegularTransactionDTO | null>(null)
const selectedBookletToLink = ref<string | null>(null)
const isLinkLoading = ref(false)

const unlinkedBooklets = computed(() => {
  if (!linkTargetTransaction.value) return []
  const linked = new Set(linkTargetTransaction.value.bookletIds ?? [])
  return booklets.value
    .filter(b => b.id !== undefined && !linked.has(String(b.id)))
    .map(b => ({ id: String(b.id), label: b.label }))
})

function getUnlinkedBookletsFor(transaction: RegularTransactionDTO) {
  const linked = new Set(transaction.bookletIds ?? [])
  return booklets.value.filter(b => b.id !== undefined && !linked.has(String(b.id)))
}

function getLinkedActiveBookletsFor(transaction: RegularTransactionDTO) {
  const ids = new Set(transaction.bookletIds ?? [])
  return booklets.value.filter(b => b.id !== undefined && ids.has(String(b.id)))
}

function openLinkDialog(transaction: RegularTransactionDTO) {
  linkTargetTransaction.value = transaction
  selectedBookletToLink.value = null
  isLinkDialogVisible.value = true
}

async function handleLink() {
  if (!linkTargetTransaction.value || !selectedBookletToLink.value) return
  isLinkLoading.value = true
  try {
    const updated = await linkRegularTransactionToBooklet(linkTargetTransaction.value.id, selectedBookletToLink.value)
    const index = transactions.value.findIndex(t => t.id === updated.id)
    if (index !== -1) transactions.value[index] = updated
    isLinkDialogVisible.value = false
    jToast.success('Livret lié avec succès')
  } catch (err: any) {
    jToast.errorAxios(err)
  } finally {
    isLinkLoading.value = false
  }
}

// ── Unlink dialog ─────────────────────────────────────────────────────────────
const isUnlinkDialogVisible = ref(false)
const unlinkTargetTransaction = ref<RegularTransactionDTO | null>(null)
const selectedBookletToUnlink = ref<string | null>(null)
const isUnlinkLoading = ref(false)

const linkedBookletsForUnlink = computed(() => {
  if (!unlinkTargetTransaction.value) return []
  const ids = unlinkTargetTransaction.value.bookletIds ?? []
  return booklets.value
    .filter(b => b.id !== undefined && ids.includes(String(b.id)))
    .map(b => ({ id: String(b.id), label: b.label }))
})

function openUnlinkDialog(transaction: RegularTransactionDTO) {
  unlinkTargetTransaction.value = transaction
  selectedBookletToUnlink.value = null
  isUnlinkDialogVisible.value = true
}

async function handleUnlink() {
  if (!unlinkTargetTransaction.value || !selectedBookletToUnlink.value) return
  isUnlinkLoading.value = true
  try {
    const updated = await unlinkRegularTransactionFromBooklet(unlinkTargetTransaction.value.id, selectedBookletToUnlink.value)
    const index = transactions.value.findIndex(t => t.id === updated.id)
    if (index !== -1) transactions.value[index] = updated
    isUnlinkDialogVisible.value = false
    jToast.success('Livret délié avec succès')
  } catch (err: any) {
    jToast.errorAxios(err)
  } finally {
    isUnlinkLoading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-4 bg-gradient-to-br from-[var(--bg-gradient-from)] to-[var(--bg-gradient-to)] px-5 md:px-6 lg:px-8 py-5 md:(py-4 h-full overflow-hidden) max-w-7xl mx-auto">
    <div class="bg-[var(--card-bg)] rounded-2xl p-5 shadow border border-[var(--card-border)] overflow-hidden">
      <div class="flex items-center gap-4 min-w-0">
        <div class="flex-1 min-w-0">
          <h1 class="text-2xl font-extrabold bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-transparent bg-clip-text m-0">
            Mes transactions régulières
          </h1>
          <div class="flex gap-4 flex-wrap mt-1">
            <span class="inline-flex items-center gap-1.5 text-sm font-semibold text-[var(--text-secondary)]">
              <i class="pi pi-sync text-[var(--primary)] text-sm" />
              {{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="flex justify-start gap-3 md:flex-row">
      <Button
        class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25 transition-all duration-300 hover:from-purple-700 hover:to-purple-900 hover:shadow-xl hover:shadow-purple-500/35 hover:-translate-y-0.5 md:w-auto md:text-3.5 md:px-4 md:py-2.5"
        icon="pi pi-plus"
        :label="isMobile ? 'Créer' : 'Créer une transaction régulière'"
        @click="openCreationRegularTransactionDialog"
      />
      <Button
        v-if="!isMobile"
        class="border-none font-semibold transition-all duration-300 md:w-auto md:text-3.5 md:px-4 md:py-2.5"
        icon="pi pi-trash"
        severity="danger"
        :disabled="selectedTransactionsCount === 0"
        :label="`Supprimer la sélection (${selectedTransactionsCount})`"
        @click="handleBulkDelete"
      />
    </div>

    <div v-if="!isMobile" class="flex-1 min-h-0 flex flex-col bg-[var(--card-bg)] rounded-2xl overflow-hidden shadow-lg border border-[var(--card-border)]">
      <AppTable
        v-model:selection="selectedTransactions"
        :columns="regularTransactionColumns"
        :rows="filteredTransactions"
        data-key="id"
        selectable
        scrollable
        scroll-height="flex"
        @row-dblclick="handleRowDoubleClick"
      >
        <template #empty>
          <div class="flex flex-col items-center justify-center p-15 text-center md:p-10">
            <i class="pi pi-sync text-4rem mb-4" style="color: var(--text-muted);" />
            <h3 class="text-1.25rem font-bold m-0 mb-2" style="color: var(--text-primary);">
              Aucune transaction régulière
            </h3>
            <p class="text-1rem m-0 mb-6" style="color: var(--text-secondary);">
              Commencez par créer votre première transaction régulière
            </p>
            <Button
              class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25"
              icon="pi pi-plus"
              label="Créer une transaction"
              @click="openCreationRegularTransactionDialog"
            />
          </div>
        </template>

        <template #body-label="{ data }">
          <div class="flex items-center gap-2">
            <span class="font-semibold" style="color: var(--text-primary);">{{ data.label }}</span>
          </div>
        </template>

        <template #body-type="{ data }">
          <Tag
            :value="data.isIncome ? 'Recette' : 'Dépense'"
            :severity="data.isIncome ? 'success' : 'danger'"
            :icon="data.isIncome ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"
          />
        </template>

        <template #body-montant="{ data }">
          <span class="font-extrabold text-1.1rem font-mono" :class="data.isIncome ? 'text-green-500 before:content-[\'+_\']' : 'text-red-500 before:content-[\'-_\']'">
            {{ Math.abs(data.value).toFixed(2) }} €
          </span>
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

        <template #body-regularity="{ data }">
          <div class="flex items-center gap-2 font-medium" style="color: var(--text-secondary);">
            <i class="pi pi-clock text-0.875rem text-purple-600" />
            <span>{{ frequencyToString(data.regularity) }}</span>
          </div>
        </template>

        <template #body-startDate="{ data }">
          <div class="flex items-center gap-2 font-medium" style="color: var(--text-secondary);">
            <i class="pi pi-calendar text-0.875rem text-purple-600" />
            <span>{{ data.startDate }}</span>
          </div>
        </template>

        <template #body-actions="{ data }">
          <div class="flex items-center gap-2">
            <Button
              v-tooltip.top="'Lier à un livret'"
              data-test="btn-link"
              icon="pi pi-link"
              size="small"
              severity="secondary"
              outlined
              :disabled="getUnlinkedBookletsFor(data).length === 0"
              @click.stop="openLinkDialog(data)"
            />
            <Button
              v-tooltip.top="'Délier d\'un livret'"
              data-test="btn-unlink"
              icon="pi pi-minus-circle"
              size="small"
              severity="warning"
              outlined
              :disabled="getLinkedActiveBookletsFor(data).length === 0"
              @click.stop="openUnlinkDialog(data)"
            />
          </div>
        </template>
      </AppTable>
      <div class="shrink-0 flex items-center justify-between flex-wrap gap-2 px-3 py-2 border-t" style="border-color: var(--card-border);">
        <div class="flex items-center gap-2">
          <span class="text-xs" style="color: var(--text-secondary);">Lignes par page&nbsp;:</span>
          <Select
            v-model="pageSize"
            :options="pageSizeOptions"
            class="w-20"
            size="small"
            @change="onRtPageSizeChange"
          />
        </div>
        <Paginator
          v-if="totalPages > 1"
          :first="currentPage * pageSize"
          :rows="pageSize"
          :total-records="totalElements"
          @page="onRtPageChange"
        />
      </div>
    </div>

    <!-- Liste des transactions (Mobile) -->
    <div v-else class="flex-1 overflow-hidden flex flex-col md:overflow-visible md:flex-none">
      <div v-if="transactions.length === 0" class="flex-1 flex flex-col items-center justify-center p-10 text-center rounded-4 shadow-lg md:p-5" style="background-color: var(--card-bg); box-shadow: 0 10px 30px var(--shadow-purple);">
        <i class="pi pi-sync text-3rem mb-4" style="color: var(--text-muted);" />
        <h3 class="text-1.125rem font-bold m-0 mb-2" style="color: var(--text-primary);">
          Aucune transaction régulière
        </h3>
        <p class="text-0.875rem m-0 mb-5" style="color: var(--text-secondary);">
          Commencez par créer votre première transaction régulière
        </p>
        <Button
          class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25"
          icon="pi pi-plus"
          label="Créer"
          @click="openCreationRegularTransactionDialog"
        />
      </div>

      <div v-else class="flex-1 overflow-y-auto p-1 flex flex-col gap-3 md:overflow-y-visible md:flex-none md:gap-4 md:p-0">
        <div
          v-for="transaction in transactions"
          :key="transaction.id"
          class="rounded-4 p-4 shadow-md border-2 border-transparent transition-all duration-300 cursor-pointer relative overflow-hidden before:content-[''] before:absolute before:left-0 before:top-0 before:bottom-0 before:w-1 before:bg-gradient-to-b before:from-purple-600 before:to-purple-800 before:transition-width before:duration-300 hover:border-purple-500 hover:bg-gradient-to-br hover:from-purple-50 hover:to-purple-50 hover:shadow-lg hover:shadow-purple-500/20 hover:before:w-1.5 active:scale-98 md:p-4.5 md:rounded-4.5 md:shadow-lg md:before:w-1.25 dark:hover:from-purple-950/30 dark:hover:to-purple-950/30"
          style="background-color: var(--card-bg); box-shadow: 0 4px 12px var(--shadow-purple); border-color: var(--card-border);"
          :class="{ 'border-purple-600 bg-gradient-to-br from-purple-50 to-purple-50 shadow-lg shadow-purple-500/20 before:w-1.5 md:shadow-xl md:shadow-purple-500/25 dark:from-purple-950/30 dark:to-purple-950/30': isSelected(transaction) }"
          @click="handleRowDoubleClick({ data: transaction })"
        >
          <div class="flex justify-between items-center mb-3 pb-3 md:mb-3.5 md:pb-3.5" style="border-bottom: 1px solid var(--border-color);">
            <div class="flex items-center gap-3">
              <Tag
                :value="transaction.isIncome ? 'Recette' : 'Dépense'"
                :severity="transaction.isIncome ? 'success' : 'danger'"
                :icon="transaction.isIncome ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"
              />
            </div>
          </div>

          <div class="flex flex-col gap-3 md:gap-3.5">
            <div class="flex items-center justify-between gap-2">
              <span class="text-1.1rem font-bold flex-1 break-words leading-1.4 md:text-1.15rem" style="color: var(--text-primary);">{{ transaction.label }}</span>
            </div>

            <div class="flex flex-col gap-2.5 md:gap-3">
              <div class="flex items-center gap-2.5 text-0.95rem md:text-1rem md:gap-3" style="color: var(--text-secondary);">
                <i class="pi pi-euro text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span class="text-1.4rem font-extrabold font-mono md:text-1.6rem" :class="transaction.isIncome ? 'text-green-500' : 'text-red-500'">
                  {{ transaction.isIncome ? '+' : '-' }}{{ Math.abs(transaction.value).toFixed(2) }} €
                </span>
              </div>

              <div class="flex items-center gap-2.5 text-0.95rem md:text-1rem md:gap-3" style="color: var(--text-secondary);">
                <i class="pi pi-clock text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span>{{ frequencyToString(transaction.regularity) }}</span>
              </div>

              <div class="flex items-center gap-2.5 text-0.95rem md:text-1rem md:gap-3" style="color: var(--text-secondary);">
                <i class="pi pi-calendar text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span>{{ transaction.startDate }}</span>
              </div>
            </div>

            <div class="flex justify-start pt-2 md:pt-2.5" style="border-top: 1px solid var(--border-color);">
              <Tag
                :value="transaction.tagDTO.label"
                :style="getTagStyle(transaction.tagDTO.colorDTO)"
                class="text-0.8rem px-3 py-1.5 md:text-0.85rem md:px-3.5 md:py-1.75"
              />
            </div>

            <div class="flex gap-2 pt-2" style="border-top: 1px solid var(--border-color);">
              <Button
                icon="pi pi-link"
                size="small"
                severity="secondary"
                outlined
                label="Lier"
                :disabled="getUnlinkedBookletsFor(transaction).length === 0"
                @click.stop="openLinkDialog(transaction)"
              />
              <Button
                v-if="getLinkedActiveBookletsFor(transaction).length > 0"
                icon="pi pi-minus-circle"
                size="small"
                severity="warning"
                outlined
                label="Délier"
                @click.stop="openUnlinkDialog(transaction)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <RegularTransactionCreationDialog
      :visible="isCreationDialogVisible"
      :booklets="booklets"
      :loading="isCreateRegularTransactionLoading"
      @create-transaction="onSave"
      @cancel-creation="cancelCreationDialog"
    />

    <RegularTransactionDialogCard
      v-model="isEditDialogVisible"
      :transaction="selectedTransaction"
      :loading="loadingTransaction"
      @save="handleEditSave"
      @delete="handleDelete"
    />

    <!-- Link dialog -->
    <Dialog
      v-model:visible="isLinkDialogVisible"
      modal
      header="🔗 Lier à un livret"
      :style="{ width: '28rem' }"
      :breakpoints="{ '575px': '90vw' }"
      :draggable="false"
    >
      <div class="flex flex-col gap-4 py-2">
        <p class="m-0 text-sm" style="color: var(--text-secondary);">
          Sélectionnez un livret à associer à cette transaction régulière.
        </p>
        <Select
          v-model="selectedBookletToLink"
          :options="unlinkedBooklets"
          option-label="label"
          option-value="id"
          placeholder="Choisir un livret"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="Annuler"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="isLinkDialogVisible = false"
        />
        <Button
          label="Lier"
          icon="pi pi-check"
          :disabled="!selectedBookletToLink || isLinkLoading"
          :loading="isLinkLoading"
          @click="handleLink"
        />
      </template>
    </Dialog>

    <!-- Unlink dialog -->
    <Dialog
      v-model:visible="isUnlinkDialogVisible"
      modal
      header="⛓️‍💥 Délier un livret"
      :style="{ width: '28rem' }"
      :breakpoints="{ '575px': '90vw' }"
      :draggable="false"
    >
      <div class="flex flex-col gap-4 py-2">
        <p class="m-0 text-sm" style="color: var(--text-secondary);">
          Sélectionnez le livret à dissocier. Les transactions virtuelles associées ne seront plus générées.
        </p>
        <Select
          v-model="selectedBookletToUnlink"
          :options="linkedBookletsForUnlink"
          option-label="label"
          option-value="id"
          placeholder="Choisir un livret"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="Annuler"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="isUnlinkDialogVisible = false"
        />
        <Button
          label="Délier"
          icon="pi pi-link"
          severity="warning"
          :disabled="!selectedBookletToUnlink || isUnlinkLoading"
          :loading="isUnlinkLoading"
          @click="handleUnlink"
        />
      </template>
    </Dialog>

    <ConfirmDialog />
  </div>
</template>

<style scoped>
:deep(.p-dropdown), :deep(.p-select), :deep(.p-calendar) {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
}
:deep(.p-inputtext), :deep(.p-dropdown-label), :deep(.p-select-label) {
  background: transparent !important;
  color: var(--text-primary) !important;
}
:deep(.p-dropdown-trigger), :deep(.p-datepicker-trigger), :deep(.p-select-trigger), :deep(.p-icon) {
  color: var(--text-secondary) !important;
}

:deep(.p-tag) {
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>
