import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({ frequencyToString: (value: string) => value }),
}))

const BOOKLETS = [
  { id: 'b-1', label: 'Compte courant', amount: 100, currency: 'EUR' },
  { id: 'b-2', label: 'Livret A', amount: 200, currency: 'EUR' },
  { id: 'b-3', label: 'Épargne', amount: 300, currency: 'EUR' },
]

function createRegularTransaction(id: string, bookletIds: string[]) {
  return {
    id,
    label: 'Loyer',
    value: 900,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-01-15',
    frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
    tagDTO: { tagId: 'tag-1', label: 'Maison', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
    bookletIds,
  }
}

const AppTableStub = {
  props: ['rows', 'columns', 'selection'],
  template: `<div><div v-for="row in rows" :key="row.id" class="app-table-row"><slot name="body-booklets" :data="row" /><slot name="body-actions" :data="row" /></div></div>`,
}

const ButtonStub = {
  props: ['label', 'icon', 'disabled'],
  emits: ['click'],
  template: `<button :disabled="disabled" @click="!disabled && $emit('click', $event)">{{ label }}<slot /></button>`,
}

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

async function settle() {
  await flushPromises()
  await flushPromises()
  await nextTick()
}

function mountPage(content: ReturnType<typeof createRegularTransaction>[], viewportWidth = 1280) {
  window.innerWidth = viewportWidth

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch: vi.fn().mockResolvedValue(BOOKLETS) }))
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
        Tag: { props: ['value'], template: '<span>{{ value }}<slot /></span>' },
        Select: true,
        ConfirmDialog: true,
        Paginator: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })
}

describe('pages/regular-transaction/index linked booklets', () => {
  beforeEach(() => vi.clearAllMocks())

  it('names every linked booklet on the row', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', ['b-1', 'b-2'])])
    await settle()

    const cell = wrapper.find('[data-test="rt-linked-booklets"]')
    expect(cell.text()).toContain('Compte courant')
    expect(cell.text()).toContain('Livret A')
    expect(cell.text()).not.toContain('Épargne')
  })

  // Saying nothing reads as "not loaded yet" rather than "linked to nothing".
  it('states explicitly when an entry is linked to no booklet', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', [])])
    await settle()

    expect(wrapper.find('[data-test="rt-linked-booklets"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="rt-no-booklet"]').text()).toMatch(/aucun livret/i)
  })

  it('names the linked booklets on the mobile card too', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', ['b-3'])], 375)
    await settle()

    expect(wrapper.find('[data-test="rt-linked-booklets-mobile"]').text()).toContain('Épargne')
  })

  it('states on the mobile card when an entry is linked to no booklet', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', [])], 375)
    await settle()

    expect(wrapper.find('[data-test="rt-no-booklet-mobile"]').text()).toMatch(/aucun livret/i)
  })

  // A disabled control that says nothing is indistinguishable from a broken one.
  it('explains why linking is unavailable when every booklet is already linked', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', ['b-1', 'b-2', 'b-3'])])
    await settle()

    expect((wrapper.vm as any).linkUnavailableReason(createRegularTransaction('rt-1', ['b-1', 'b-2', 'b-3'])))
      .toMatch(/déjà liée/i)
    expect(wrapper.find('[data-test="rt-link-wrapper"]').attributes('title')).toMatch(/déjà liée/i)
  })

  it('explains why unlinking is unavailable when nothing is linked', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', [])])
    await settle()

    expect(wrapper.find('[data-test="rt-unlink-wrapper"]').attributes('title')).toMatch(/aucun livret/i)
  })

  it('carries the same explanation on mobile', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', ['b-1', 'b-2', 'b-3'])], 375)
    await settle()

    expect(wrapper.find('[data-test="rt-link-wrapper-mobile"]').attributes('title')).toMatch(/déjà liée/i)
  })

  it('gives no explanation while the action is available', async () => {
    const wrapper = mountPage([createRegularTransaction('rt-1', ['b-1'])])
    await settle()

    expect((wrapper.vm as any).linkUnavailableReason(createRegularTransaction('rt-1', ['b-1']))).toBe('')
  })
})
