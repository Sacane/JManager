import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import useAdmin from '~/composables/useAdmin'
import useAuth from '~/composables/useAuth'
import AdminPage from '../../pages/admin/index.vue'

vi.mock('~/composables/useAuth')
vi.mock('~/composables/useAdmin')

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

/** Renders the loading and empty slots the way PrimeVue's DataTable would. */
const AppTableStub = {
  props: ['rows', 'loading'],
  template: `<div class="app-table">
    <div v-if="loading" class="table-loading"><slot name="loading" /></div>
    <div v-if="!rows || rows.length === 0" class="table-empty"><slot name="empty" /></div>
  </div>`,
}

const PassThrough = { template: '<div><slot /></div>' }

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(() => resolve()))
}

async function settle() {
  for (let i = 0; i < 6; i++) await flushPromises()
  await nextTick()
}

function mountPage(options: { viewportWidth?: number, flagsFetching?: boolean } = {}) {
  window.innerWidth = options.viewportWidth ?? 1280

  const isLoading = ref(false)
  let pending = deferred<void>()
  // Mirrors useAdmin: loading is true for exactly as long as the request is in flight.
  const fetchUsers = vi.fn(async () => {
    isLoading.value = true
    await pending.promise
    isLoading.value = false
  })

  vi.mocked(useAuth).mockReturnValue({
    user: ref(null) as any,
    isAuthenticated: ref(true) as any,
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    isAdmin: ref(true) as any,
    tryRefresh: vi.fn(),
    initializeSession: vi.fn(),
  } as any)

  vi.mocked(useAdmin).mockReturnValue({
    users: ref([]) as any,
    totalUsers: ref(0) as any,
    totalPages: ref(0) as any,
    currentPage: ref(0) as any,
    pageSize: ref(10) as any,
    isLoading: isLoading as any,
    fetchUsers,
    createUser: vi.fn(),
  } as any)

  vi.stubGlobal('useFeatureFlags', () => ({
    flags: ref([]),
    isFetching: ref(options.flagsFetching ?? false),
    isToggling: ref(false),
    isEnabled: vi.fn(() => false),
    fetchFlags: vi.fn(),
    toggleFlag: vi.fn(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), warn: vi.fn(), error: vi.fn(), errorAxios: vi.fn() }))

  const wrapper = shallowMount(AdminPage, {
    global: {
      stubs: {
        AppTable: AppTableStub,
        ProgressSpinner: { template: '<div class="spinner" />' },
        Tabs: PassThrough,
        TabList: PassThrough,
        Tab: PassThrough,
        TabPanels: PassThrough,
        TabPanel: PassThrough,
      },
    },
  })

  return {
    wrapper,
    fetchUsers,
    answer: () => pending.resolve(),
    nextRequest: () => {
      pending = deferred<void>()
    },
  }
}

describe('pages/admin/index loading', () => {
  beforeEach(() => vi.clearAllMocks())

  // An admin page always lists at least its own admin, so "Aucun utilisateur trouvé" during the
  // first load was never true.
  it('shows the shape of the user list, not the empty message, before the first load answers', async () => {
    const { wrapper } = mountPage()
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Aucun utilisateur trouvé')
  })

  it('shows the empty message once a load has answered with nothing', async () => {
    const { wrapper, answer } = mountPage()
    answer()
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Aucun utilisateur trouvé')
  })

  it('keeps the spinner overlay for a later page change', async () => {
    const { wrapper, answer, nextRequest } = mountPage()
    answer()
    await settle()

    nextRequest()
    const pending = (wrapper.vm as any).loadUsers(1)
    await nextTick()

    // The request is genuinely in flight, or the absence below would prove nothing.
    expect(wrapper.find('.table-loading').exists()).toBe(true)
    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)

    answer()
    await pending
  })

  it('shows row placeholders on mobile instead of a spinner', async () => {
    const { wrapper } = mountPage({ viewportWidth: 375 })
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(true)
    expect(wrapper.find('.spinner').exists()).toBe(false)
  })

  it('shows row placeholders for the feature flags before any is loaded', async () => {
    const { wrapper } = mountPage({ flagsFetching: true })
    await settle()

    const skeletons = wrapper.findAll('[data-test="page-skeleton"]')
    expect(skeletons.some(skeleton => skeleton.text().includes('Chargement des flags'))).toBe(true)
    expect(wrapper.find('.spinner').exists()).toBe(false)
  })
})
