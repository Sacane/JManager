import { flushPromises, shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { MAX_BOOKLETS } from '../../constants/booklet'
import BookletPage from '../../pages/booklet/index.vue'

const fetchMock = vi.fn()

vi.mock('../../composables/useBooklet', () => ({
  default: () => ({
    fetch: fetchMock,
    deleteBooklet: vi.fn(),
    createBooklet: vi.fn(),
  }),
}))

function makeBooklets(count: number) {
  return Array.from({ length: count }, (_, index) => ({
    id: `booklet-${index}`,
    label: `Livret ${index}`,
    amount: 100,
    currency: '€',
  }))
}

async function mountWith(count: number) {
  fetchMock.mockResolvedValue(makeBooklets(count))
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('useRouter', () => ({ push: vi.fn() }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), warn: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))

  const wrapper = shallowMount(BookletPage, {
    global: {
      // capitalizeFirst is called from the template, so it has to be a context mock: a
      // stubGlobal is invisible to _ctx and the render throws, which silently freezes the DOM
      // on its previous state while the component state keeps updating.
      mocks: { capitalizeFirst: (value: string) => value },
      stubs: {
        ConfirmDialog: true,
        BookletBookingDialog: true,
        ProgressSpinner: true,
        Button: {
          props: ['label', 'disabled'],
          // PrimeVue renders the label as the button text; the stub has to do the same for the
          // assertions on the disabled action to mean anything.
          template: '<button :disabled="disabled" v-bind="$attrs">{{ label }}<slot /></button>',
        },
      },
    },
  })
  // flushPromises resolves the load; the re-render it triggers needs its own tick before the
  // DOM reflects the loaded booklets.
  await flushPromises()
  await nextTick()

  return wrapper
}

describe('pages/booklet/index booklet limit', () => {
  it('declares the maximum once, as a named constant', () => {
    expect(MAX_BOOKLETS).toBe(6)
  })

  it('shows the remaining slots below the limit', async () => {
    const wrapper = await mountWith(MAX_BOOKLETS - 2)

    expect(wrapper.text()).toContain(`${MAX_BOOKLETS - 2}/${MAX_BOOKLETS}`)
    expect(wrapper.text()).toContain('2 emplacements restants')
  })

  // The disabled button used to say "Limite atteinte" and nothing else: no reason, no way out.
  it('explains the limit and the way forward once it is reached', async () => {
    const wrapper = await mountWith(MAX_BOOKLETS)
    const text = wrapper.text()

    expect(text).toContain('Limite atteinte')
    expect(text).toMatch(new RegExp(`${MAX_BOOKLETS} livrets`))
    expect(text).toMatch(/supprim/i)
  })

  it('attaches the explanation to the disabled action for assistive technology', async () => {
    const wrapper = await mountWith(MAX_BOOKLETS)
    const hint = wrapper.find('[data-test="booklet-limit-hint"]')
    const action = wrapper.find('[data-test="booklet-limit-action"]')

    expect(hint.exists()).toBe(true)
    expect(action.attributes('aria-describedby')).toBe(hint.attributes('id'))
  })

  it('offers no explanation while creation is still possible', async () => {
    const wrapper = await mountWith(MAX_BOOKLETS - 1)

    expect(wrapper.find('[data-test="booklet-limit-hint"]').exists()).toBe(false)
  })
})
