import type { TransactionSearchResponse } from '../../composables/useTransactionSearch'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import TransactionSearchField from '../../components/booklet/TransactionSearchField.vue'
import SearchPage from '../../pages/search.vue'

const searchTransactions = vi.fn()
const findAllBooklet = vi.fn()
const toastError = vi.fn()

function found(label: string, date: string, bookletId = 'b-1', bookletLabel = 'Compte courant', isIncome = false) {
  return {
    transaction: { id: label + date, label, value: '60.00', isIncome, date, isPreview: false, regularTransactionId: null },
    bookletId,
    bookletLabel,
  }
}

const ANSWER: TransactionSearchResponse = {
  from: '2025-11-01',
  to: '2026-10-31',
  regulars: [{
    regularTransactionId: 'rent',
    label: 'Loyer',
    bookletId: 'b-1',
    bookletLabel: 'Compte courant',
    occurrenceCount: 12,
    firstDate: '2025-11-05',
    lastDate: '2026-10-05',
    occurrences: [found('Loyer', '2026-10-05'), found('Loyer', '2026-09-05')],
  }],
  months: [
    { month: '2026-10', transactions: [found('Loyer parking', '2026-10-03')] },
    { month: '2026-08', transactions: [found('Remb. loyer', '2026-08-12', 'b-2', 'Livret A', true), found('Loyer garage', '2026-08-02')] },
  ],
  hasOlderTransactions: true,
}

function mountPage() {
  vi.stubGlobal('useTransactionSearch', () => ({ searchTransactions }))
  vi.stubGlobal('useBooklet', () => ({ findAllBooklet }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), warn: vi.fn(), error: toastError, errorAxios: vi.fn() }))
  return shallowMount(SearchPage, {
    global: {
      stubs: {
        Select: { props: ['modelValue', 'options'], emits: ['update:modelValue'], template: '<select data-test="booklet-filter" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option></select>' },
        Button: { props: ['label'], emits: ['click'], template: '<button @click="$emit(\'click\')">{{ label }}<slot /></button>' },
        TransactionSearchField: false,
      },
      components: { TransactionSearchField },
    },
  })
}

async function typeAndWait(wrapper: ReturnType<typeof mountPage>, text: string) {
  await wrapper.find('[data-test="transaction-search-input"]').setValue(text)
  await vi.advanceTimersByTimeAsync(300)
  await flushPromises()
}

describe('pages/search', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    searchTransactions.mockResolvedValue(ANSWER)
    findAllBooklet.mockResolvedValue([{ id: 'b-1', label: 'Compte courant' }, { id: 'b-2', label: 'Livret A' }])
  })

  afterEach(() => vi.useRealTimers())

  it('asks for something to search before searching anything', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('[data-test="search-hint"]').exists()).toBe(true)
    expect(searchTransactions).not.toHaveBeenCalled()
  })

  it('searches every booklet over the last twelve months once typing pauses', async () => {
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    expect(searchTransactions).toHaveBeenCalledTimes(1)
    expect(searchTransactions).toHaveBeenCalledWith('loyer', 12, undefined)
    expect(wrapper.find('[data-test="search-window"]').text()).toContain('12 derniers mois')
  })

  it('shows each regular transaction once, with its count and span', async () => {
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    const regular = wrapper.find('[data-test="regular-match"]')
    expect(regular.text()).toContain('Loyer')
    expect(regular.text()).toContain('Compte courant')
    expect(regular.text()).toContain('12 occurrences')
    expect(regular.text()).toContain('nov. 2025 → oct. 2026')
    expect(wrapper.findAll('[data-test="regular-occurrence"]')).toHaveLength(0)
  })

  it('unfolds the occurrences of a regular transaction on demand', async () => {
    const wrapper = mountPage()
    await typeAndWait(wrapper, 'loyer')

    await wrapper.find('[data-test="regular-toggle"]').trigger('click')

    expect(wrapper.find('[data-test="regular-toggle"]').attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('[data-test="regular-occurrence"]')).toHaveLength(2)
  })

  it('groups the other matches by month, newest first, with a count', async () => {
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    const headers = wrapper.findAll('[data-test="month-header"]').map(header => header.text())
    expect(headers).toEqual(['Octobre 2026 · 1 résultat', 'Août 2026 · 2 résultats'])
  })

  it('shows each match with its date, booklet and signed amount', async () => {
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    const rows = wrapper.findAll('[data-test="search-row"]')
    expect(rows[1].text()).toContain('12/08')
    expect(rows[1].text()).toContain('Remb. loyer')
    expect(rows[1].text()).toContain('Livret A')
    expect(rows[1].find('[data-test="search-row-amount"]').text()).toBe('+60,00 €')
    expect(rows[1].find('[data-test="search-row-amount"]').classes()).toContain('amount-positive')
    expect(rows[2].find('[data-test="search-row-amount"]').classes()).toContain('amount-negative')
  })

  it('opens the booklet at the month of the match', async () => {
    const wrapper = mountPage()
    await typeAndWait(wrapper, 'loyer')

    await wrapper.findAll('[data-test="search-row"]')[1].trigger('click')

    expect(navigateTo).toHaveBeenCalledWith({ path: '/booklet/b-2', query: { month: 8, year: 2026 } })
  })

  it('searches twelve months further back on request', async () => {
    const wrapper = mountPage()
    await typeAndWait(wrapper, 'loyer')

    await wrapper.find('[data-test="search-further"]').trigger('click')
    await flushPromises()

    expect(searchTransactions).toHaveBeenLastCalledWith('loyer', 24, undefined)
  })

  it('does not offer to search further back when nothing older exists', async () => {
    searchTransactions.mockResolvedValue({ ...ANSWER, hasOlderTransactions: false })
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    expect(wrapper.find('[data-test="search-further"]').exists()).toBe(false)
  })

  it('narrows the search to one booklet', async () => {
    const wrapper = mountPage()
    await flushPromises()
    await typeAndWait(wrapper, 'loyer')

    await wrapper.find('[data-test="booklet-filter"]').setValue('b-2')
    await flushPromises()

    expect(searchTransactions).toHaveBeenLastCalledWith('loyer', 12, 'b-2')
  })

  it('says nothing matches over the searched months, and still offers to look further', async () => {
    searchTransactions.mockResolvedValue({ ...ANSWER, regulars: [], months: [] })
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'introuvable')

    expect(wrapper.find('[data-test="search-no-match"]').text()).toContain('« introuvable » sur les 12 derniers mois')
    expect(wrapper.find('[data-test="search-further"]').exists()).toBe(true)
  })

  // A new search starts again over the last twelve months.
  it('restarts from twelve months when the search changes', async () => {
    const wrapper = mountPage()
    await typeAndWait(wrapper, 'loyer')
    await wrapper.find('[data-test="search-further"]').trigger('click')
    await flushPromises()

    await typeAndWait(wrapper, 'essence')

    expect(searchTransactions).toHaveBeenLastCalledWith('essence', 12, undefined)
  })

  it('reports a failed search', async () => {
    searchTransactions.mockRejectedValue(new Error('down'))
    const wrapper = mountPage()

    await typeAndWait(wrapper, 'loyer')

    expect(toastError).toHaveBeenCalled()
  })

  it('clears the results with the search', async () => {
    const wrapper = mountPage()
    await typeAndWait(wrapper, 'loyer')

    await typeAndWait(wrapper, '')

    expect(wrapper.findAll('[data-test="search-row"]')).toHaveLength(0)
    expect(wrapper.find('[data-test="search-hint"]').exists()).toBe(true)
  })

  it('waits for a pause before searching', async () => {
    const wrapper = mountPage()
    await wrapper.find('[data-test="transaction-search-input"]').setValue('lo')
    await nextTick()

    expect(searchTransactions).not.toHaveBeenCalled()
  })
})
