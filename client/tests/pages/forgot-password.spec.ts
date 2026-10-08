import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import ForgotPasswordPage from '../../pages/forgot-password.vue'

const InputTextStub = {
  props: ['modelValue', 'type', 'placeholder', 'maxlength', 'autocomplete'],
  emits: ['update:modelValue'],
  template: '<input :type="type" :value="modelValue" :maxlength="maxlength" :autocomplete="autocomplete" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  props: ['type', 'label', 'loading', 'disabled'],
  template: '<button :type="type" :disabled="disabled"><slot />{{ label }}</button>',
}

const NuxtLinkStub = { props: ['to'], template: '<a :href="to"><slot /></a>' }

const requestReset = vi.fn()
const toastError = vi.fn()

function mountPage(rememberedEmail = '') {
  vi.stubGlobal('usePasswordReset', vi.fn(() => ({
    isRequesting: ref(false),
    requestReset,
    takeRememberedEmail: vi.fn(() => rememberedEmail),
  })))
  vi.stubGlobal('useJToast', vi.fn(() => ({ success: vi.fn(), warn: vi.fn(), error: toastError, errorAxios: vi.fn() })))
  return shallowMount(ForgotPasswordPage, {
    global: { stubs: { InputText: InputTextStub, Button: ButtonStub, NuxtLink: NuxtLinkStub } },
  })
}

async function submit(wrapper: ReturnType<typeof mountPage>, email?: string) {
  if (email !== undefined) await wrapper.find('[data-test="reset-email-input"]').setValue(email)
  await wrapper.find('form').trigger('submit')
  await nextTick()
  await nextTick()
}

describe('pages/forgot-password', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    requestReset.mockResolvedValue('sent')
  })

  it('starts from the address typed on the sign-in form', () => {
    const wrapper = mountPage('johan@example.com')

    expect((wrapper.find('[data-test="reset-email-input"]').element as HTMLInputElement).value).toBe('johan@example.com')
  })

  it('bounds the address like every other email field', () => {
    const input = mountPage().find('[data-test="reset-email-input"]')

    expect(input.attributes('maxlength')).toBe('255')
    expect(input.attributes('autocomplete')).toBe('email')
  })

  it('confirms without saying whether an account exists', async () => {
    const wrapper = mountPage()

    await submit(wrapper, ' johan@example.com ')

    expect(requestReset).toHaveBeenCalledWith('johan@example.com')
    const sent = wrapper.find('[data-test="reset-request-sent"]').text()
    expect(sent).toContain('Si un compte correspond à johan@example.com')
    expect(sent).toContain('30 minutes')
    expect(sent).toContain('courriers indésirables')
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('asks for an address before sending anything', async () => {
    const wrapper = mountPage()

    await submit(wrapper, '   ')

    expect(requestReset).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="reset-email-error"]').text()).toContain('adresse e-mail')
  })

  it('asks to wait when too many requests were made, without a generic error', async () => {
    requestReset.mockResolvedValue('rate_limited')
    const wrapper = mountPage()

    await submit(wrapper, 'johan@example.com')

    expect(wrapper.find('[data-test="reset-email-error"]').text()).toContain('quelques minutes')
    expect(toastError).not.toHaveBeenCalled()
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('reports an unexpected failure and keeps the form', async () => {
    requestReset.mockResolvedValue('error')
    const wrapper = mountPage()

    await submit(wrapper, 'johan@example.com')

    expect(toastError).toHaveBeenCalled()
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('lets the user try another address after sending', async () => {
    const wrapper = mountPage()
    await submit(wrapper, 'johan@example.com')

    await wrapper.find('[data-test="reset-try-another"]').trigger('click')

    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('leads back to sign-in', () => {
    expect(mountPage().find('a[href="/login"]').exists()).toBe(true)
  })
})
