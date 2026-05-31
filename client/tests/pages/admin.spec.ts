import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import useAdmin from '~/composables/useAdmin'
import useAuth from '~/composables/useAuth'
import AdminPage from '../../pages/admin/index.vue'

// ---------------------------------------------------------------------------
// Module mocks — required for explicitly-imported composables.
// vi.stubGlobal only patches globalThis, not ES-module bindings.
// ---------------------------------------------------------------------------
vi.mock('~/composables/useAuth')
vi.mock('~/composables/useAdmin')

// ---------------------------------------------------------------------------
// PrimeVue stubs
// ---------------------------------------------------------------------------
const InputTextStub = {
  name: 'InputText',
  props: ['modelValue', 'disabled', 'type', 'placeholder', 'id'],
  emits: ['update:modelValue'],
  template: '<input :id="id" :type="type || \'text\'" :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'icon', 'loading', 'disabled', 'type', 'severity', 'outlined', 'rounded', 'text'],
  emits: ['click'],
  template: '<button :type="$props.type || \'button\'" :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
}

// Tabs stubs must render their default slot so child content stays in the DOM.
const TabsStub = {
  name: 'Tabs',
  props: ['value'],
  template: '<div class="tabs-stub"><slot /></div>',
}
const TabListStub = {
  name: 'TabList',
  template: '<div class="tab-list-stub"><slot /></div>',
}
const TabStub = {
  name: 'Tab',
  props: ['value'],
  template: '<button class="tab-stub"><slot /></button>',
}
const TabPanelsStub = {
  name: 'TabPanels',
  template: '<div class="tab-panels-stub"><slot /></div>',
}
const TabPanelStub = {
  name: 'TabPanel',
  props: ['value'],
  template: '<div class="tab-panel-stub"><slot /></div>',
}

// ---------------------------------------------------------------------------
describe('pages/admin/index', () => {
  let createUserMock: ReturnType<typeof vi.fn>
  let toastrErrorMock: ReturnType<typeof vi.fn>
  let toastrSuccessMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    createUserMock = vi.fn()
    toastrErrorMock = vi.fn()
    toastrSuccessMock = vi.fn()

    vi.mocked(useAuth).mockReturnValue({
      user: ref(null) as any,
      isAuthenticated: ref(false) as any,
      login: vi.fn(),
      logout: vi.fn(),
      register: vi.fn(),
      isAdmin: ref(true) as any,
      tryRefresh: vi.fn(),
      initializeSession: vi.fn(),
    })

    vi.mocked(useAdmin).mockReturnValue({
      users: ref([]) as any,
      totalUsers: ref(0) as any,
      totalPages: ref(0) as any,
      currentPage: ref(0) as any,
      pageSize: ref(10) as any,
      isLoading: ref(false) as any,
      fetchUsers: vi.fn(),
      createUser: createUserMock,
    } as any)

    vi.stubGlobal('useJToast', () => ({
      success: toastrSuccessMock,
      warn: () => {},
      error: toastrErrorMock,
      errorAxios: () => {},
    }))

    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: (_scope: string) => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
  })

  function mountPage() {
    return shallowMount(AdminPage, {
      global: {
        stubs: {
          InputText: InputTextStub,
          Button: ButtonStub,
          Tabs: TabsStub,
          TabList: TabListStub,
          Tab: TabStub,
          TabPanels: TabPanelsStub,
          TabPanel: TabPanelStub,
        },
      },
    })
  }

  /**
   * Set form values via the component's public VM proxy.
   * In Vue 3 with <script setup>, refs are auto-unwrapped on wrapper.vm,
   * so wrapper.vm.newUser gives the reactive object directly.
   */
  async function setFormValues(
    wrapper: ReturnType<typeof mountPage>,
    values: { username?: string, email?: string, password?: string, confirmPassword?: string },
  ) {
    const vm = wrapper.vm as any
    Object.assign(vm.newUser, values)
    await nextTick()
  }

  // --- Rendering ---

  it('renders both the user management form and the feature flags panel', () => {
    const wrapper = mountPage()
    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('.flags-card').exists()).toBe(true)
  })

  it('renders all four form fields including email', () => {
    const wrapper = mountPage()
    const stubs = wrapper.findAllComponents(InputTextStub)
    const ids = stubs.map(s => s.props('id'))
    expect(ids).toContain('username')
    expect(ids).toContain('email')
    expect(ids).toContain('password')
    expect(ids).toContain('confirmPassword')
  })

  it('renders the feature flags empty state when no flags are configured', () => {
    // useFeatureFlags global stub (setup.ts) returns empty flags by default
    const wrapper = mountPage()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.flag-list').exists()).toBe(false)
  })

  it('renders the flag list when flags are available', () => {
    vi.stubGlobal('useFeatureFlags', vi.fn(() => ({
      flags: ref([{ key: 'EMAIL_VERIFICATION', enabled: false }]) as any,
      isFetching: ref(false),
      isToggling: ref(false),
      isEnabled: vi.fn(() => false),
      fetchFlags: vi.fn(),
      toggleFlag: vi.fn(),
    })))

    const wrapper = mountPage()
    expect(wrapper.find('.flag-list').exists()).toBe(true)
    expect(wrapper.find('.empty-state').exists()).toBe(false)
  })

  // --- Validation ---

  it('shows an error when required fields are empty on submit', async () => {
    const wrapper = mountPage()

    await wrapper.find('form').trigger('submit')

    expect(toastrErrorMock).toHaveBeenCalledWith('Veuillez remplir tous les champs')
    expect(createUserMock).not.toHaveBeenCalled()
  })

  it('shows an error when email is empty', async () => {
    const wrapper = mountPage()
    await setFormValues(wrapper, { username: 'alice', password: 'password123', confirmPassword: 'password123' })

    await wrapper.find('form').trigger('submit')

    expect(toastrErrorMock).toHaveBeenCalledWith('Veuillez remplir tous les champs')
    expect(createUserMock).not.toHaveBeenCalled()
  })

  it('shows an error when the email format is invalid', async () => {
    const wrapper = mountPage()
    await setFormValues(wrapper, { username: 'alice', email: 'not-an-email', password: 'password123', confirmPassword: 'password123' })

    await wrapper.find('form').trigger('submit')

    expect(toastrErrorMock).toHaveBeenCalledWith("L'adresse email n'est pas valide")
    expect(createUserMock).not.toHaveBeenCalled()
  })

  it('shows an error when passwords do not match', async () => {
    const wrapper = mountPage()
    await setFormValues(wrapper, { username: 'alice', email: 'alice@example.com', password: 'password123', confirmPassword: 'different' })

    await wrapper.find('form').trigger('submit')

    expect(toastrErrorMock).toHaveBeenCalledWith('Les mots de passe ne correspondent pas')
    expect(createUserMock).not.toHaveBeenCalled()
  })

  it('shows an error when the password is shorter than 6 characters', async () => {
    const wrapper = mountPage()
    await setFormValues(wrapper, { username: 'alice', email: 'alice@example.com', password: 'abc', confirmPassword: 'abc' })

    await wrapper.find('form').trigger('submit')

    expect(toastrErrorMock).toHaveBeenCalledWith('Le mot de passe doit contenir au moins 6 caractères')
    expect(createUserMock).not.toHaveBeenCalled()
  })

  // --- Successful submission ---

  it('calls createUser with username, email, password and confirmPassword on valid form', async () => {
    const wrapper = mountPage()
    await setFormValues(wrapper, {
      username: 'alice',
      email: 'alice@example.com',
      password: 'password123',
      confirmPassword: 'password123',
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(toastrErrorMock).not.toHaveBeenCalled()
    expect(createUserMock).toHaveBeenCalledWith(
      expect.objectContaining({
        username: 'alice',
        email: 'alice@example.com',
        password: 'password123',
        confirmPassword: 'password123',
      }),
      expect.any(Function),
      expect.any(Function),
    )
  })
})
