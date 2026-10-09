import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import FieldError from '../../components/FieldError.vue'
import PasswordField from '../../components/PasswordField.vue'
import PasswordRules from '../../components/PasswordRules.vue'
import ResetPasswordPage from '../../pages/reset-password.vue'

const InputTextStub = {
  props: ['modelValue', 'type', 'placeholder', 'maxlength', 'disabled', 'autocomplete'],
  emits: ['update:modelValue'],
  template: '<input :type="type" :value="modelValue" :maxlength="maxlength" :autocomplete="autocomplete" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  props: ['type', 'label', 'loading', 'disabled'],
  template: '<button :type="type" :disabled="disabled"><slot />{{ label }}</button>',
}

const NuxtLinkStub = { props: ['to'], template: '<a :href="to"><slot /></a>' }

const COMPLIANT = 'brand-new-password'

const checkLink = vi.fn()
const confirmReset = vi.fn()
const toastSuccess = vi.fn()
const toastError = vi.fn()

function openAt(url: string) {
  window.history.replaceState({ position: 3 }, '', url)
}

async function mountPage() {
  vi.stubGlobal('usePasswordReset', vi.fn(() => ({
    isCheckingLink: ref(false),
    isConfirming: ref(false),
    checkLink,
    confirmReset,
  })))
  vi.stubGlobal('useJToast', vi.fn(() => ({ success: toastSuccess, warn: vi.fn(), error: toastError, errorAxios: vi.fn() })))
  const wrapper = shallowMount(ResetPasswordPage, {
    global: {
      stubs: {
        InputText: InputTextStub,
        Button: ButtonStub,
        NuxtLink: NuxtLinkStub,
        PasswordField: false,
        PasswordRules: false,
        FieldError: false,
      },
      components: { PasswordField, PasswordRules, FieldError },
    },
  })
  await flushPromises()
  return wrapper
}

async function fillAndSubmit(wrapper: Awaited<ReturnType<typeof mountPage>>, newPassword: string, confirmPassword = newPassword) {
  const vm = wrapper.vm as unknown as { newPassword: string, confirmPassword: string }
  vm.newPassword = newPassword
  vm.confirmPassword = confirmPassword
  await nextTick()
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('pages/reset-password', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    checkLink.mockResolvedValue('valid')
    confirmReset.mockResolvedValue({ kind: 'reset' })
    openAt('/reset-password#token=abc')
  })

  afterEach(() => openAt('/'))

  // The page may be left open, bookmarked or shared from the address bar.
  it('removes the token from the address bar and the history entry', async () => {
    await mountPage()

    expect(window.location.hash).toBe('')
    expect(window.location.pathname).toBe('/reset-password')
    expect(window.history.state).toEqual({ position: 3 })
    expect(checkLink).toHaveBeenCalledWith('abc')
  })

  it('shows the new password form with the password rules when the link is valid', async () => {
    const wrapper = await mountPage()

    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('[data-test="password-rules"]').text()).toContain('Au moins 12 caractères')
  })

  it('bounds both password fields and asks managers for a new password', async () => {
    const wrapper = await mountPage()

    const inputs = wrapper.findAll('input')
    expect(inputs).toHaveLength(2)
    for (const input of inputs) {
      expect(input.attributes('maxlength')).toBe('100')
      expect(input.attributes('autocomplete')).toBe('new-password')
    }
  })

  it.each([
    ['expired', 'reset-link-expired'],
    ['invalid', 'reset-link-invalid'],
  ])('offers a new link and no form when the link is %s', async (status, dataTest) => {
    checkLink.mockResolvedValue(status)

    const wrapper = await mountPage()

    expect(wrapper.find(`[data-test="${dataTest}"]`).exists()).toBe(true)
    expect(wrapper.find('a[href="/forgot-password"]').exists()).toBe(true)
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('treats a link without a token as invalid, without asking the server', async () => {
    openAt('/reset-password')

    const wrapper = await mountPage()

    expect(checkLink).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="reset-link-invalid"]').exists()).toBe(true)
  })

  // The link may still be good: saying it is dead would send the user to request another one for nothing.
  it('offers to check again when the server cannot answer now', async () => {
    checkLink.mockResolvedValueOnce('unavailable').mockResolvedValueOnce('valid')
    const wrapper = await mountPage()
    expect(wrapper.find('form').exists()).toBe(false)

    await wrapper.find('[data-test="reset-retry-check"]').trigger('click')
    await flushPromises()

    expect(checkLink).toHaveBeenLastCalledWith('abc')
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('reports mismatched passwords under the confirmation and sends nothing', async () => {
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, COMPLIANT, 'something-else-entirely')

    expect(wrapper.find('[data-test="confirm-password-error"]').text()).toContain('ne correspondent pas')
    expect(confirmReset).not.toHaveBeenCalled()
  })

  it('reports a password breaking the rules under the new password and sends nothing', async () => {
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, 'short-pass')

    expect(wrapper.find('[data-test="new-password-error"]').text()).toContain('au moins 12 caractères')
    expect(confirmReset).not.toHaveBeenCalled()
  })

  it('places a server refusal by the password policy under the new password', async () => {
    confirmReset.mockResolvedValue({
      kind: 'refused',
      payload: { errorKey: 'domain.user.password.policy_violation', reasons: ['equals_email'] },
    })
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, COMPLIANT)

    expect(wrapper.find('[data-test="new-password-error"]').text())
      .toBe('Le mot de passe doit être différent de votre adresse e-mail.')
  })

  it('switches to the expired state when the link dies while the user types', async () => {
    confirmReset.mockResolvedValue({ kind: 'expired' })
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, COMPLIANT)

    expect(wrapper.find('[data-test="reset-link-expired"]').exists()).toBe(true)
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('asks to wait when too many attempts were made', async () => {
    confirmReset.mockResolvedValue({ kind: 'rate_limited' })
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, COMPLIANT)

    expect(wrapper.find('[data-test="reset-form-error"]').text()).toContain('quelques minutes')
  })

  it('takes the user to sign in with the new password once reset', async () => {
    const wrapper = await mountPage()

    await fillAndSubmit(wrapper, COMPLIANT)

    expect(confirmReset).toHaveBeenCalledWith('abc', COMPLIANT, COMPLIANT)
    expect(toastSuccess).toHaveBeenCalledWith(expect.stringContaining('nouveau mot de passe'))
    expect(navigateTo).toHaveBeenCalledWith('/login')
  })
})
