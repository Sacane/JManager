import type { PasswordPolicy } from '../../utils/passwordPolicy'
import { describe, expect, it } from 'vitest'
import {
  PASSWORD_POLICY_ERROR_KEY,
  passwordRuleLabel,
  passwordRuleProblem,
  passwordStrength,
  policyViolationMessage,
  unmetPasswordRules,
} from '../../utils/passwordPolicy'

const POLICY: PasswordPolicy = { minLength: 12, maxLength: 100, rules: ['too_short', 'too_long', 'equals_email'] }

describe('unmetPasswordRules', () => {
  it('accepts a password of the minimum length', () => {
    expect(unmetPasswordRules('correcthorse', 'johan@example.com', POLICY)).toEqual([])
  })

  it('reports a password one character short', () => {
    expect(unmetPasswordRules('correcthors', null, POLICY)).toEqual(['too_short'])
  })

  it('reports a password over the maximum length', () => {
    expect(unmetPasswordRules('a'.repeat(101), null, POLICY)).toEqual(['too_long'])
  })

  // The server counts code points: `length` would count an emoji twice and accept 11 characters.
  it('counts an emoji as one character, like the server', () => {
    const eleven = 'élève-été🔒x'
    expect(unmetPasswordRules(eleven, null, POLICY)).toEqual(['too_short'])
    expect(unmetPasswordRules(`${eleven}y`, null, POLICY)).toEqual([])
  })

  it('reports the account address used as password, ignoring case', () => {
    expect(unmetPasswordRules('johan.doe@example.com', 'Johan.Doe@Example.com', POLICY)).toEqual(['equals_email'])
  })

  it('reports every unmet rule', () => {
    expect(unmetPasswordRules('a@b.fr', 'a@b.fr', POLICY)).toEqual(['too_short', 'equals_email'])
  })

  it('ignores the address rule when no address is known', () => {
    expect(unmetPasswordRules('correcthorse', '', POLICY)).toEqual([])
  })
})

describe('passwordRuleLabel', () => {
  it('states the lengths the server published', () => {
    expect(passwordRuleLabel('too_short', POLICY)).toBe('Au moins 12 caractères')
    expect(passwordRuleLabel('too_long', POLICY)).toBe('Au plus 100 caractères')
    expect(passwordRuleLabel('equals_email', POLICY)).toBe('Différent de votre adresse e-mail')
  })
})

describe('passwordStrength', () => {
  it('has no level for an empty password', () => {
    expect(passwordStrength('', POLICY)).toBe('none')
  })

  it('is weak below the minimum length, whatever the characters', () => {
    expect(passwordStrength('Ab1!Ab1!', POLICY)).toBe('weak')
  })

  it('is fair at the minimum length with one kind of character', () => {
    expect(passwordStrength('correcthorse', POLICY)).toBe('fair')
  })

  it('is strong well above the minimum length', () => {
    expect(passwordStrength('correct horse battery', POLICY)).toBe('strong')
  })

  it('is strong at the minimum length with varied characters', () => {
    expect(passwordStrength('Correct-hors3', POLICY)).toBe('strong')
  })
})

describe('policyViolationMessage', () => {
  it('words every unmet rule the server reported', () => {
    const payload = { errorKey: PASSWORD_POLICY_ERROR_KEY, reasons: ['too_short', 'equals_email'] }

    expect(policyViolationMessage(payload, POLICY))
      .toBe('Le mot de passe doit contenir au moins 12 caractères et être différent de votre adresse e-mail.')
  })

  it('still words the rule when the policy could not be loaded', () => {
    const payload = { errorKey: PASSWORD_POLICY_ERROR_KEY, reasons: ['too_short'] }

    expect(policyViolationMessage(payload, null)).toBe('Le mot de passe est trop court.')
  })

  it('returns nothing for another error', () => {
    expect(policyViolationMessage({ errorKey: 'domain.user.password.mismatch' }, POLICY)).toBeNull()
    expect(policyViolationMessage(undefined, POLICY)).toBeNull()
  })

  it('falls back to a general sentence when the reasons are missing', () => {
    expect(policyViolationMessage({ errorKey: PASSWORD_POLICY_ERROR_KEY }, POLICY))
      .toBe('Le mot de passe ne respecte pas les règles de sécurité.')
  })
})

describe('passwordRuleProblem', () => {
  it('words the unmet rules of a password', () => {
    expect(passwordRuleProblem('a@b.fr', 'a@b.fr', POLICY))
      .toBe('Le mot de passe doit contenir au moins 12 caractères et être différent de votre adresse e-mail.')
  })

  it('has nothing to say about a compliant password', () => {
    expect(passwordRuleProblem('correct horse battery', 'a@b.fr', POLICY)).toBeNull()
  })

  // Without the published policy there is nothing to check against: the server decides alone.
  it('has nothing to say when the policy is unknown', () => {
    expect(passwordRuleProblem('a', null, null)).toBeNull()
  })
})
