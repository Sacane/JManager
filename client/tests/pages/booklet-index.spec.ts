import { flushPromises, shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import BookletPage from '../../pages/booklet/index.vue'

const fetchMock = vi.fn().mockResolvedValue([])
const deleteBookletMock = vi.fn().mockResolvedValue(undefined)
const createBookletMock = vi.fn().mockResolvedValue(undefined)

vi.mock('../../composables/useBooklet', () => ({
  default: () => ({
    fetch: fetchMock,
    deleteBooklet: deleteBookletMock,
    createBooklet: createBookletMock,
  }),
}))

const successToast = vi.fn()
const errorAxiosToast = vi.fn()

function mountBookletPage(activeScopes: string[] = []) {
  successToast.mockClear()
  errorAxiosToast.mockClear()
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('useRouter', () => ({ push: vi.fn() }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('useJToast', () => ({
    success: successToast,
    error: vi.fn(),
    warn: vi.fn(),
    errorAxios: errorAxiosToast,
  }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))

  const wrapper = shallowMount(BookletPage, {
    global: {
      mocks: {
        capitalizeFirst: (value: string) => value,
      },
      stubs: {
        ConfirmDialog: true,
        ProgressSpinner: { template: '<div class="spinner" />' },
        BookletBookingDialog: true,
        Button: {
          props: ['label', 'loading', 'disabled'],
          template: '<button :data-label="label" :data-loading="String(loading)" :disabled="disabled"><slot /></button>',
        },
      },
    },
  })

  return { wrapper }
}

describe('pages/booklet/index loading states', () => {
  it('shows list loading feedback when account load scope is active', () => {
    const { wrapper } = mountBookletPage(['booklet.index.load'])

    expect(wrapper.text()).toContain('Chargement des livrets...')
  })

  it('shows add button in loading state when account creation scope is active', () => {
    const { wrapper } = mountBookletPage(['booklet.index.create'])

    const addButton = wrapper.findAll('button').find(btn => btn.attributes('data-label') === 'Nouveau livret')
    expect(addButton).toBeDefined()
    expect(addButton?.attributes('data-loading')).toBe('true')
    expect(addButton?.attributes('disabled')).toBeDefined()
  })

  it('hides the empty state while booklets are loading', () => {
    const { wrapper } = mountBookletPage(['booklet.index.load'])

    expect(wrapper.find('.empty-state').exists()).toBe(false)
  })
})

describe('pages/booklet/index empty state', () => {
  it('shows the empty state alone, without the booklets grid and its add card', async () => {
    fetchMock.mockResolvedValueOnce([])
    const { wrapper } = mountBookletPage()

    await flushPromises()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.booklets-container').exists()).toBe(false)
    expect(wrapper.find('.add-card').exists()).toBe(false)
  })

  it('shows the booklets grid instead of the empty state when booklets exist', async () => {
    fetchMock.mockResolvedValueOnce([
      { id: 1, label: 'Livret A', amount: 1500, currency: '€' },
    ])
    const { wrapper } = mountBookletPage()

    await flushPromises()

    expect(wrapper.find('.empty-state').exists()).toBe(false)
    expect(wrapper.find('.booklets-container').exists()).toBe(true)
    expect(wrapper.find('.add-card').exists()).toBe(true)
  })
})

describe('pages/booklet/index user feedback', () => {
  it('accepts a booklet created with a zero amount', async () => {
    const { wrapper } = mountBookletPage()
    await flushPromises()

    await wrapper.findComponent({ name: 'BookletBookingDialog' })
      .vm
      .$emit('createBooklet', { label: 'Livret vide', digit: 0 })
    await flushPromises()

    expect(createBookletMock).toHaveBeenCalledWith('Livret vide', 0, '€')
    expect(successToast).toHaveBeenCalled()
    expect(errorAxiosToast).not.toHaveBeenCalled()
  })

  it('shows an error toast when the booklet creation fails', async () => {
    createBookletMock.mockRejectedValueOnce(new Error('creation failed'))
    const { wrapper } = mountBookletPage()
    await flushPromises()

    await wrapper.findComponent({ name: 'BookletBookingDialog' })
      .vm
      .$emit('createBooklet', { label: 'Livret KO', digit: 10 })
    await flushPromises()

    expect(errorAxiosToast).toHaveBeenCalled()
    expect(successToast).not.toHaveBeenCalled()
  })

  it('shows an error toast when the booklets load fails', async () => {
    fetchMock.mockRejectedValueOnce(new Error('load failed'))
    mountBookletPage()

    await flushPromises()

    expect(errorAxiosToast).toHaveBeenCalled()
  })
})
