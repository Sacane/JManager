import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LEGACY_BOOKLET_ORDER_KEY, useBookletOrder } from '../../composables/useBookletOrder'
import BookletPage from '../../pages/booklet/index.vue'

const fetchMock = vi.fn()
const reorderBooklets = vi.fn()
const push = vi.fn()
const mounted: Array<{ unmount: () => void }> = []

vi.mock('../../composables/useBooklet', () => ({
  default: () => ({
    fetch: fetchMock,
    deleteBooklet: vi.fn(),
    createBooklet: vi.fn(),
  }),
}))

function aBooklet(id: string, label: string, displayPosition: number | null = null) {
  return { id, label, amount: 100, currency: '€', transactions: [], displayPosition }
}

async function mountPage(booklets = [aBooklet('a', 'Compte courant'), aBooklet('b', 'Livret A'), aBooklet('c', 'Compte joint')]) {
  fetchMock.mockResolvedValue(booklets)
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useRouter', () => ({ push }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), warn: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  // The real ordering, saving through a fake API.
  vi.stubGlobal('useBooklet', () => ({ reorderBooklets }))
  vi.stubGlobal('useBookletOrder', useBookletOrder)

  const wrapper = shallowMount(BookletPage, {
    global: {
      mocks: { capitalizeFirst: (value: string) => value },
      stubs: {
        BookletBookingDialog: true,
        BookletDeleteDialog: true,
        Button: {
          props: ['label', 'loading', 'disabled', 'icon'],
          emits: ['click'],
          template: '<button :data-label="label" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>',
        },
      },
    },
    attachTo: document.body,
  })
  mounted.push(wrapper)
  await flushPromises()
  return wrapper
}

type Page = Awaited<ReturnType<typeof mountPage>>

const labels = (wrapper: Page) => wrapper.findAll('.booklet-name').map(name => name.text())
const toggle = (wrapper: Page) => wrapper.find('[data-test="reorder-toggle"]')

describe('pages/booklet/index reorder mode', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    reorderBooklets.mockResolvedValue(undefined)
    window.localStorage.clear()
  })

  afterEach(() => mounted.splice(0).forEach(wrapper => wrapper.unmount()))

  it('offers to reorganise once there are two booklets', async () => {
    const wrapper = await mountPage()

    expect(toggle(wrapper).attributes('data-label')).toBe('Réorganiser')
    expect(toggle(wrapper).attributes('aria-pressed')).toBe('false')
  })

  it('does not offer it for a single booklet', async () => {
    const wrapper = await mountPage([aBooklet('a', 'Compte courant')])

    expect(toggle(wrapper).exists()).toBe(false)
  })

  // The old "Réorganiser" icon was a drag handle a click did nothing with.
  it('no longer shows a drag handle outside the mode, nor lets the cards be dragged', async () => {
    const wrapper = await mountPage()

    expect(wrapper.find('.drag-handle').exists()).toBe(false)
    expect(wrapper.findAll('.booklet-card:not(.add-card)').every(card => card.attributes('draggable') === 'false')).toBe(true)
    expect(wrapper.find('[data-test="move-after-a"]').exists()).toBe(false)
  })

  it('shows a move before and a move after on each booklet, and nothing that deletes or adds', async () => {
    const wrapper = await mountPage()

    await toggle(wrapper).trigger('click')

    expect(toggle(wrapper).attributes('data-label')).toBe('Terminé')
    expect(toggle(wrapper).attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('[data-test="move-before-a"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="move-after-a"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-test="move-after-c"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="move-after-a"]').attributes('aria-label')).toBe('Déplacer « Compte courant » après')
    expect(wrapper.find('.delete-btn').exists()).toBe(false)
    expect(wrapper.find('.add-card').exists()).toBe(false)
    expect(wrapper.find('[data-test="reorder-hint"]').exists()).toBe(true)
  })

  it('moves a booklet one step and keeps the order with the account', async () => {
    const wrapper = await mountPage()
    await toggle(wrapper).trigger('click')

    await wrapper.find('[data-test="move-after-a"]').trigger('click')
    await flushPromises()

    expect(labels(wrapper)).toEqual(['Livret A', 'Compte courant', 'Compte joint'])
    expect(reorderBooklets).toHaveBeenCalledWith(['b', 'a', 'c'])
    expect(wrapper.find('[data-test="reorder-announcement"]').text()).toBe('« Compte courant » est maintenant en position 2 sur 3.')
  })

  it('keeps the focus on the arrow that was used, or on the other one once at the end', async () => {
    const wrapper = await mountPage()
    await toggle(wrapper).trigger('click')

    await wrapper.find('[data-test="move-after-a"]').trigger('click')
    await flushPromises()
    expect(document.activeElement?.getAttribute('data-test')).toBe('move-after-a')

    await wrapper.find('[data-test="move-after-a"]').trigger('click')
    await flushPromises()
    expect(document.activeElement?.getAttribute('data-test')).toBe('move-before-a')
  })

  it('lets the cards be dragged in the mode only', async () => {
    const wrapper = await mountPage()

    await toggle(wrapper).trigger('click')

    expect(wrapper.findAll('.booklet-card').every(card => card.attributes('draggable') === 'true')).toBe(true)
  })

  it('does not open a booklet while reorganising, and opens it again once done', async () => {
    const wrapper = await mountPage()
    await toggle(wrapper).trigger('click')

    await wrapper.find('.booklet-card').trigger('click')
    expect(push).not.toHaveBeenCalled()

    await toggle(wrapper).trigger('click')
    await wrapper.find('.booklet-card').trigger('click')
    expect(push).toHaveBeenCalledWith('/booklet/a')
    expect(wrapper.find('[data-test="move-after-a"]').exists()).toBe(false)
  })

  it('takes over the order kept in this browser by an earlier version', async () => {
    window.localStorage.setItem(LEGACY_BOOKLET_ORDER_KEY, JSON.stringify(['c', 'a', 'b']))

    const wrapper = await mountPage()

    expect(reorderBooklets).toHaveBeenCalledWith(['c', 'a', 'b'])
    expect(labels(wrapper)).toEqual(['Compte joint', 'Compte courant', 'Livret A'])
  })
})
