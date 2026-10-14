<script setup lang="ts">
import type { FoundTransactionDTO, RegularTransactionMatchDTO, SearchedTransactionDTO, TransactionSearchResponse } from '~/composables/useTransactionSearch'
import authMiddleware from '~/middleware/auth'
import { formatSignedAmount } from '~/utils/money'
import { capitalizeFirst } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const SEARCH_DEBOUNCE_MS = 300
const DEFAULT_MONTHS = 12
const MONTHS_STEP = 12
const MAX_MONTHS = 120

const { searchTransactions } = useTransactionSearch()
const { findAllBooklet } = useBooklet()
const toast = useJToast()

const { search, appliedSearch } = useDebouncedSearch(SEARCH_DEBOUNCE_MS)
const months = ref(DEFAULT_MONTHS)
const bookletId = ref('')
const result = ref<TransactionSearchResponse | null>(null)
const isSearching = ref(false)
const unfolded = ref<Set<string>>(new Set())
const bookletOptions = ref<{ label: string, value: string }[]>([{ label: 'Tous les comptes', value: '' }])

// A slower answer to an older search must not overwrite the newer one.
let latestRequest = 0

async function runSearch() {
  if (!appliedSearch.value) {
    result.value = null
    return
  }
  const request = ++latestRequest
  isSearching.value = true
  try {
    const answer = await searchTransactions(appliedSearch.value, months.value, bookletId.value || undefined)
    if (request === latestRequest) result.value = answer
  } catch {
    if (request === latestRequest) toast.error('La recherche n\'a pas pu aboutir. Veuillez réessayer.')
  } finally {
    if (request === latestRequest) isSearching.value = false
  }
}

// A new search, or another booklet, starts again over the last twelve months.
function restartSearch() {
  months.value = DEFAULT_MONTHS
  unfolded.value = new Set()
  runSearch()
}
watch(appliedSearch, restartSearch)
watch(bookletId, restartSearch)

function searchFurther() {
  months.value = Math.min(months.value + MONTHS_STEP, MAX_MONTHS)
  runSearch()
}

const canSearchFurther = computed(() => !!result.value?.hasOlderTransactions && months.value < MAX_MONTHS)
const hasMatches = computed(() => !!result.value && (result.value.regulars.length > 0 || result.value.months.length > 0))
const windowLabel = computed(() => `${months.value} derniers mois`)

const regularKey = (regular: RegularTransactionMatchDTO) => `${regular.regularTransactionId}:${regular.bookletId}`
const isUnfolded = (regular: RegularTransactionMatchDTO) => unfolded.value.has(regularKey(regular))
function toggle(regular: RegularTransactionMatchDTO) {
  const next = new Set(unfolded.value)
  const key = regularKey(regular)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  unfolded.value = next
}

// Dates arrive as "yyyy-MM-dd": read as written, never through a time zone.
function parts(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return { year: year!, month: month!, day: day! }
}
const MONTH_LONG = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })
const MONTH_SHORT = new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric' })
function monthOf(isoDate: string, format: Intl.DateTimeFormat) {
  const { year, month } = parts(`${isoDate}-01`.slice(0, 10))
  return format.format(new Date(year, month - 1, 1))
}
function monthHeader(yearMonth: string, count: number) {
  return `${capitalizeFirst(monthOf(yearMonth, MONTH_LONG))} · ${count} résultat${count > 1 ? 's' : ''}`
}
function span(regular: RegularTransactionMatchDTO) {
  return `${monthOf(regular.firstDate, MONTH_SHORT)} → ${monthOf(regular.lastDate, MONTH_SHORT)}`
}
function dayAndMonth(isoDate: string) {
  const { month, day } = parts(isoDate)
  return `${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}`
}
const amountOf = (transaction: FoundTransactionDTO) => formatSignedAmount(transaction.value, transaction.isIncome)
const amountClass = (transaction: FoundTransactionDTO) => (transaction.isIncome ? 'amount-positive' : 'amount-negative')

function openInBooklet(match: SearchedTransactionDTO) {
  const { year, month } = parts(match.transaction.date)
  navigateTo({ path: `/booklet/${match.bookletId}`, query: { month, year } })
}

onMounted(async () => {
  try {
    const booklets = await findAllBooklet()
    bookletOptions.value = [
      { label: 'Tous les comptes', value: '' },
      ...booklets.map(booklet => ({ label: booklet.label, value: String(booklet.id) })),
    ]
  } catch {
    // The filter is optional: without it the search still covers every booklet.
  }
})
</script>

<template>
  <div class="page-shell max-w-[1100px]">
    <div class="page-header">
      <div>
        <h1 class="page-heading">
          Rechercher
        </h1>
        <p class="page-subheading">
          Retrouvez une transaction dans tous vos comptes.
        </p>
      </div>
    </div>

    <div class="flex flex-col gap-2 mb-6 md:(flex-row items-center gap-3)">
      <TransactionSearchField v-model="search" class="md:max-w-md" />
      <Select
        v-model="bookletId"
        :options="bookletOptions"
        option-label="label"
        option-value="value"
        aria-label="Compte recherché"
        class="w-full md:w-56"
      />
    </div>

    <!-- Nothing searched yet -->
    <div v-if="!appliedSearch" class="card p-8 text-center" data-test="search-hint">
      <i class="pi pi-search text-4xl text-[var(--text-muted)]" aria-hidden="true" />
      <p class="text-[var(--text-secondary)] mt-4 mb-0">
        Tapez une partie du libellé d'une transaction pour la retrouver dans tous vos comptes.
      </p>
    </div>

    <!-- First answer pending -->
    <div v-else-if="isSearching && !result" class="card p-4">
      <PageSkeleton variant="list" :count="6" label="Recherche en cours…" />
    </div>

    <template v-else-if="result">
      <p class="text-sm text-[var(--text-secondary)] mb-4" data-test="search-window" role="status">
        Sur les {{ windowLabel }}
      </p>

      <div v-if="!hasMatches" class="card p-8 text-center" data-test="search-no-match">
        <i class="pi pi-search text-4xl text-[var(--text-muted)]" aria-hidden="true" />
        <p class="text-[var(--text-secondary)] mt-4 mb-0 break-words">
          Aucune transaction ne correspond à « {{ appliedSearch }} » sur les {{ windowLabel }}.
        </p>
      </div>

      <!-- Regular transactions: one line each, whatever the number of months they span -->
      <section v-if="result.regulars.length > 0" class="mb-6" aria-labelledby="regular-matches-title">
        <h2 id="regular-matches-title" class="block-title mb-3">
          Transactions régulières
        </h2>
        <ul class="card divide-y divide-[var(--card-border)] list-none m-0 p-0">
          <li v-for="regular in result.regulars" :key="regularKey(regular)" data-test="regular-match">
            <button
              type="button"
              class="w-full flex items-center gap-3 px-4 py-3 text-left bg-transparent border-0 cursor-pointer hover:bg-[var(--card-hover-bg)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
              :aria-expanded="isUnfolded(regular)"
              :aria-controls="`occurrences-${regularKey(regular)}`"
              data-test="regular-toggle"
              @click="toggle(regular)"
            >
              <i class="pi text-xs text-[var(--text-secondary)]" :class="isUnfolded(regular) ? 'pi-chevron-down' : 'pi-chevron-right'" aria-hidden="true" />
              <span class="flex-1 min-w-0">
                <span class="block font-semibold text-[var(--text-primary)] truncate">{{ regular.label }}</span>
                <span class="block text-xs text-[var(--text-secondary)]">
                  {{ regular.bookletLabel }} · {{ regular.occurrenceCount }} occurrence{{ regular.occurrenceCount > 1 ? 's' : '' }} · {{ span(regular) }}
                </span>
              </span>
              <span :class="amountClass(regular.occurrences[0]!.transaction)" class="shrink-0">
                {{ amountOf(regular.occurrences[0]!.transaction) }}
              </span>
            </button>
            <ul v-if="isUnfolded(regular)" :id="`occurrences-${regularKey(regular)}`" class="list-none m-0 p-0 pb-2">
              <li v-for="occurrence in regular.occurrences" :key="occurrence.transaction.id ?? occurrence.transaction.date" data-test="regular-occurrence">
                <button
                  type="button"
                  class="w-full flex items-center gap-3 pl-11 pr-4 py-1.5 text-left text-sm bg-transparent border-0 cursor-pointer hover:bg-[var(--card-hover-bg)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
                  @click="openInBooklet(occurrence)"
                >
                  <span class="w-12 shrink-0 text-[var(--text-secondary)]">{{ dayAndMonth(occurrence.transaction.date) }}</span>
                  <span class="flex-1 text-[var(--text-secondary)]">{{ monthOf(occurrence.transaction.date, MONTH_SHORT) }}</span>
                  <span :class="amountClass(occurrence.transaction)">{{ amountOf(occurrence.transaction) }}</span>
                </button>
              </li>
            </ul>
          </li>
        </ul>
      </section>

      <!-- One-off transactions, month by month -->
      <section v-for="group in result.months" :key="group.month" class="mb-6" :aria-label="monthHeader(group.month, group.transactions.length)">
        <h2 class="text-sm font-bold uppercase tracking-wide text-[var(--text-secondary)] mb-2" data-test="month-header">
          {{ monthHeader(group.month, group.transactions.length) }}
        </h2>
        <ul class="card divide-y divide-[var(--card-border)] list-none m-0 p-0">
          <li v-for="match in group.transactions" :key="match.transaction.id ?? `${match.bookletId}-${match.transaction.date}-${match.transaction.label}`">
            <button
              type="button"
              class="w-full flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-3 text-left bg-transparent border-0 cursor-pointer hover:bg-[var(--card-hover-bg)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
              data-test="search-row"
              @click="openInBooklet(match)"
            >
              <span class="w-12 shrink-0 text-sm text-[var(--text-secondary)]">{{ dayAndMonth(match.transaction.date) }}</span>
              <span class="flex-1 min-w-0">
                <span class="block font-semibold text-[var(--text-primary)] truncate">{{ match.transaction.label }}</span>
                <span class="block text-xs text-[var(--text-secondary)]">
                  {{ match.bookletLabel }}<template v-if="match.transaction.isPreview"> · prévisionnelle</template>
                </span>
              </span>
              <span :class="amountClass(match.transaction)" class="shrink-0" data-test="search-row-amount">
                {{ amountOf(match.transaction) }}
              </span>
            </button>
          </li>
        </ul>
      </section>

      <div v-if="canSearchFurther" class="flex justify-center mt-2">
        <Button
          class="btn-outline-primary"
          icon="pi pi-history"
          label="Chercher plus loin"
          :loading="isSearching"
          data-test="search-further"
          @click="searchFurther"
        />
      </div>
    </template>
  </div>
</template>
