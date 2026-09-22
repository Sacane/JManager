import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import PasswordRules from '../../components/PasswordRules.vue'

const PUBLISHED = { minLength: 12, maxLength: 100, rules: ['too_short', 'too_long', 'equals_email'] }

function mountRules(props: Record<string, unknown> = {}, policy: unknown = PUBLISHED) {
  const load = vi.fn().mockResolvedValue(undefined)
  vi.stubGlobal('usePasswordPolicy', () => ({ policy: ref(policy), load }))
  const wrapper = mount(PasswordRules, { props: { password: '', ...props } })
  return { wrapper, load }
}

function rule(wrapper: ReturnType<typeof mountRules>['wrapper'], key: string) {
  return wrapper.find(`[data-test="rule-${key}"]`)
}

describe('components/PasswordRules', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads the published policy', () => {
    const { load } = mountRules()

    expect(load).toHaveBeenCalled()
  })

  it('states the rules before anything is typed, with the published length', () => {
    const { wrapper } = mountRules({ email: 'johan@example.com' })

    expect(rule(wrapper, 'too_short').text()).toContain('Au moins 12 caractères')
    expect(rule(wrapper, 'equals_email').text()).toContain('Différent de votre adresse e-mail')
    expect(rule(wrapper, 'too_short').attributes('data-met')).toBe('false')
  })

  // The field's maxlength already stops at the maximum: announcing it would only add noise.
  it('does not list the maximum length', () => {
    const { wrapper } = mountRules()

    expect(rule(wrapper, 'too_long').exists()).toBe(false)
  })

  it('lists the address rule only when an address is known', () => {
    const { wrapper } = mountRules({ email: '' })

    expect(rule(wrapper, 'too_short').exists()).toBe(true)
    expect(rule(wrapper, 'equals_email').exists()).toBe(false)
  })

  it('ticks each rule as it is satisfied', async () => {
    const { wrapper } = mountRules({ email: 'johan@example.com', password: 'short' })
    expect(rule(wrapper, 'too_short').attributes('data-met')).toBe('false')
    expect(rule(wrapper, 'equals_email').attributes('data-met')).toBe('true')

    await wrapper.setProps({ password: 'correct horse battery' })

    expect(rule(wrapper, 'too_short').attributes('data-met')).toBe('true')
  })

  it('unticks the address rule when the address is typed as the password', async () => {
    const { wrapper } = mountRules({ email: 'johan.doe@example.com', password: 'Johan.Doe@example.com' })

    expect(rule(wrapper, 'equals_email').attributes('data-met')).toBe('false')
  })

  it('says whether each rule is met to a screen reader, not only through colour', async () => {
    const { wrapper } = mountRules({ email: 'johan@example.com', password: 'johan@example.com' })

    expect(rule(wrapper, 'too_short').text()).toContain('(respectée)')
    expect(rule(wrapper, 'too_short').text()).not.toContain('(non respectée)')
    expect(rule(wrapper, 'equals_email').text()).toContain('(non respectée)')
  })

  it('lists the difference from the current password when one is given', async () => {
    const { wrapper } = mountRules({ currentPassword: 'old-password-12', password: 'old-password-12' })
    expect(rule(wrapper, 'differs_from_current').attributes('data-met')).toBe('false')

    await wrapper.setProps({ password: 'new-password-123' })

    expect(rule(wrapper, 'differs_from_current').attributes('data-met')).toBe('true')
    expect(rule(wrapper, 'differs_from_current').text()).toContain('Différent de votre mot de passe actuel')
  })

  it('highlights the unmet rules when asked to', () => {
    const { wrapper } = mountRules({ password: 'short', showErrors: true })

    expect(rule(wrapper, 'too_short').classes()).toContain('password-rule--unmet')
  })

  it('does not highlight unmet rules by default, while the user is still typing', () => {
    const { wrapper } = mountRules({ password: 'short' })

    expect(rule(wrapper, 'too_short').classes()).not.toContain('password-rule--unmet')
  })

  it('shows no strength before anything is typed', () => {
    const { wrapper } = mountRules()

    expect(wrapper.find('[data-test="password-strength"]').exists()).toBe(false)
  })

  it('shows the strength of what is typed', async () => {
    const { wrapper } = mountRules({ password: 'short' })
    expect(wrapper.find('[data-test="password-strength"]').attributes('data-level')).toBe('weak')
    expect(wrapper.find('[data-test="password-strength"]').text()).toContain('faible')

    await wrapper.setProps({ password: 'correct horse battery' })

    expect(wrapper.find('[data-test="password-strength"]').attributes('data-level')).toBe('strong')
    expect(wrapper.find('[data-test="password-strength"]').text()).toContain('élevée')
  })

  // The server still enforces the policy; a checklist with invented numbers would be worse than none.
  it('shows nothing when the policy could not be loaded', () => {
    const { wrapper } = mountRules({ password: 'short' }, null)

    expect(wrapper.find('[data-test="password-rules"]').exists()).toBe(false)
  })
})
