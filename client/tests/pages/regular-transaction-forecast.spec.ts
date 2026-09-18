import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({ frequencyToString: (value: string) => value }),
}))

function createRegularTransaction(overrides: Record<string, unknown> = {}) {
  return {
    id: 'rt-1',
    label: 'Loyer',
    value: 900,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-01-15',
    frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
    tagDTO: { tagId: 'tag-1', label: 'Maison', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
    bookletIds: ['booklet-1'],
    ...overrides,
  }
}

const AppTableStub = {
  props: ['rows', 'columns', 'selection'],
  template: `<div><div v-for="row in rows" :key="row.id" class="app-table-row"><slot name="body-nextOccurrence" :data="row" /></div></div>`,
}

const ButtonStub = {
  props: ['label', 'icon', 'disabled'],
  emits: ['click'],
  template: `<button :disabled="disabled" @click="!disabled && $emit('click', $event)">{{ label }}<slot /></button>`,
}

/** Microtask based: `setTimeout` is frozen by the fake timers these tests rely on. */
function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(() => resolve()))
}

async function settle() {
  for (let i = 0; i < 6; i++) await flushPromises()
  await nextTick()
}

function mountPage(content: ReturnType<typeof createRegularTransaction>[], viewportWidth = 1280) {
  window.innerWidth = viewportWidth

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction: vi.fn().mockResolvedValue({
      content,
      pageNumber: 0,
      pageSize: 10,
      totalElements: content.length,
      totalPages: 1,
    }),
    saveMonthlyTransaction: vi.fn(),
    getRegularTransactionById: vi.fn(),
    updateRegularTransaction: vi.fn(),
    deleteRegularTransaction: vi.fn(),
    deleteRegularTransactions: vi.fn(),
    linkRegularTransactionToBooklet: vi.fn(),
    unlinkRegularTransactionFromBooklet: vi.fn(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))

  return shallowMount(RegularTransactionPage, {
    global: {
      directives: { tooltip: {} },
      stubs: {
        AppTable: AppTableStub,
        Button: ButtonStub,
        Tag: { template: '<span><slot /></span>' },
        Select: true,
        ConfirmDialog: true,
        Paginator: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })
}

describe('pages/regular-transaction/index forecast', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 2, 10, 12, 0, 0))
  })

  afterEach(() => vi.useRealTimers())

  it('shows the next occurrence of every entry', async () => {
    const wrapper = mountPage([createRegularTransaction()])
    await settle()

    expect(wrapper.find('[data-test="rt-next-occurrence"]').text()).toContain('15/03/2026')
  })

  it('marks an ended recurrence instead of inventing a next occurrence', async () => {
    const wrapper = mountPage([
      createRegularTransaction({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-02-20' } }),
    ])
    await settle()

    expect(wrapper.find('[data-test="rt-next-occurrence"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="rt-ended"]').exists()).toBe(true)
  })

  // The commitment counts what actually falls in the month. A weekly charge commits four or five
  // times; it is never turned into a fractional monthly equivalent.
  it('sums the expenses actually falling in the current month', async () => {
    const wrapper = mountPage([
      createRegularTransaction({ id: 'rt-1', value: 900, isIncome: false }),
      createRegularTransaction({ id: 'rt-2', value: 10, isIncome: false, regularity: 'WEEKLY', startDate: '2026-03-02' }),
    ])
    await settle()

    // 900 once, plus 10 on the 2nd, 9th, 16th, 23rd and 30th of March.
    expect((wrapper.vm as any).monthlyCommitment.expenses).toBe(950)
    expect(wrapper.find('[data-test="rt-monthly-expenses"]').text()).toContain('950')
  })

  it('sums the income separately', async () => {
    const wrapper = mountPage([
      createRegularTransaction({ id: 'rt-1', value: 2000, isIncome: true }),
      createRegularTransaction({ id: 'rt-2', value: 900, isIncome: false }),
    ])
    await settle()

    expect((wrapper.vm as any).monthlyCommitment.income).toBe(2000)
    expect((wrapper.vm as any).monthlyCommitment.expenses).toBe(900)
  })

  it('leaves an ended recurrence out of the commitment', async () => {
    const wrapper = mountPage([
      createRegularTransaction({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-02-20' } }),
    ])
    await settle()

    expect((wrapper.vm as any).monthlyCommitment.expenses).toBe(0)
  })

  it('shows the next occurrence on the mobile card too', async () => {
    const wrapper = mountPage([createRegularTransaction()], 375)
    await settle()

    expect(wrapper.find('[data-test="rt-next-occurrence-mobile"]').text()).toContain('15/03/2026')
  })
})
