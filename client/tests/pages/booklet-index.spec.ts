import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AccountPage from '../../pages/booklet/index.vue'

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

function mountAccountPage(activeScopes: string[] = []) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('useRouter', () => ({ push: vi.fn() }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))

  const wrapper = shallowMount(AccountPage, {
    global: {
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
    const { wrapper } = mountAccountPage(['account.index.load'])

    expect(wrapper.text()).toContain('Chargement des livrets...')
  })

  it('shows add button in loading state when account creation scope is active', () => {
    const { wrapper } = mountAccountPage(['account.index.create'])

    const addButton = wrapper.findAll('button').find(btn => btn.attributes('data-label') === 'Nouveau livret')
    expect(addButton).toBeDefined()
    expect(addButton?.attributes('data-loading')).toBe('true')
    expect(addButton?.attributes('disabled')).toBeDefined()
  })
})
