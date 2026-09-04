import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import ConfirmByTypingDialog from '../../components/ConfirmByTypingDialog.vue'
import SettingsPage from '../../pages/settings/index.vue'

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({
    getSettings: vi.fn().mockResolvedValue({ projectionWindowDays: 15, bookletCycles: [] }),
    updateSettings: vi.fn(),
  }),
}))

vi.mock('~/composables/useChangePassword', () => ({
  default: () => ({
    currentPassword: ref(''),
    newPassword: ref(''),
    confirmPassword: ref(''),
    confirmPasswordError: ref(null),
    isSubmitting: ref(false),
    changePassword: vi.fn(),
  }),
}))

const DialogStub = {
  name: 'Dialog',
  props: ['visible', 'header'],
  template: '<section v-if="visible"><h2>{{ header }}</h2><slot /><slot name="footer" /></section>',
}

const InputTextStub = {
  name: 'InputText',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" v-bind="$attrs" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" v-bind="$attrs" @click="$emit(\'click\')">{{ label }}</button>',
}

let deleteAccountMock: ReturnType<typeof vi.fn>
let errorToast: ReturnType<typeof vi.fn>

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

async function mountSettings(username: string | null = 'johan') {
  vi.stubGlobal('useAuth', () => ({
    user: ref(username ? { id: '1', username, email: 'johan@example.com', roles: [] } : null),
    isAuthenticated: ref(true),
    login: vi.fn(),
    logout: vi.fn(),
    deleteAccount: deleteAccountMock,
    register: vi.fn(),
    isAdmin: ref(false),
    tryRefresh: vi.fn(),
    initializeSession: vi.fn(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: errorToast }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('onBeforeRouteLeave', vi.fn())

  const wrapper = shallowMount(SettingsPage, {
    global: {
      components: { ConfirmByTypingDialog },
      stubs: {
        ConfirmByTypingDialog: false,
        PasswordField: true,
        Dialog: DialogStub,
        InputText: InputTextStub,
        Button: ButtonStub,
      },
    },
  })
  await flushPromises()
  await nextTick()

  return wrapper
}

describe('pages/settings/index my account', () => {
  beforeEach(() => {
    deleteAccountMock = vi.fn().mockResolvedValue(true)
    errorToast = vi.fn()
  })

  it('shows the account identity', async () => {
    const wrapper = await mountSettings()
    const section = wrapper.find('[data-test="account-section"]')

    expect(section.exists()).toBe(true)
    expect(section.text()).toContain('johan')
    expect(section.text()).toContain('johan@example.com')
  })

  it('does not delete anything before the dialog is confirmed', async () => {
    const wrapper = await mountSettings()

    await wrapper.find('[data-test="open-delete-account"]').trigger('click')

    expect(deleteAccountMock).not.toHaveBeenCalled()
  })

  it('requires the username to be retyped', async () => {
    const wrapper = await mountSettings()
    await wrapper.find('[data-test="open-delete-account"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="confirm-danger-action"]').attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="confirm-typed-word"]').setValue('johan')

    expect(wrapper.find('[data-test="confirm-danger-action"]').attributes('disabled')).toBeUndefined()
  })

  it('deletes the account once confirmed', async () => {
    const wrapper = await mountSettings()
    await wrapper.find('[data-test="open-delete-account"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="confirm-typed-word"]').setValue('johan')
    await wrapper.find('[data-test="confirm-danger-action"]').trigger('click')
    await flushPromises()

    expect(deleteAccountMock).toHaveBeenCalledTimes(1)
  })

  // A failed deletion must be visible; the user is still signed in and needs to know why.
  it('reports a failed deletion and keeps the dialog open', async () => {
    deleteAccountMock.mockResolvedValue(false)
    const wrapper = await mountSettings()
    await wrapper.find('[data-test="open-delete-account"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="confirm-typed-word"]').setValue('johan')
    await wrapper.find('[data-test="confirm-danger-action"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(errorToast).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-test="confirm-typed-word"]').exists()).toBe(true)
  })

  it('states what the deletion destroys', async () => {
    const wrapper = await mountSettings()
    await wrapper.find('[data-test="open-delete-account"]').trigger('click')
    await nextTick()

    const text = wrapper.text()
    expect(text).toMatch(/irréversible/i)
    expect(text).toMatch(/livrets|transactions/i)
  })
})
