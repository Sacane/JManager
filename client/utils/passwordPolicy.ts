/**
 * The password rules as the API publishes them (`GET /api/password-policy`). The numbers always come from
 * the server: a copy kept here is how a 6-character minimum came to be announced that nothing enforced.
 */
export type PasswordRuleKey = 'too_short' | 'too_long' | 'equals_email'

export interface PasswordPolicy {
  minLength: number
  maxLength: number
  rules: PasswordRuleKey[]
}

export type PasswordStrength = 'none' | 'weak' | 'fair' | 'strong'

/** The error key the API returns when a new password breaks the policy. */
export const PASSWORD_POLICY_ERROR_KEY = 'domain.user.password.policy_violation'

// Beyond the minimum, this many extra characters make a password strong on their own.
const STRONG_LENGTH_MARGIN = 4
// This many kinds of character (lower case, upper case, digit, other) make a minimum-length password strong.
const STRONG_CHARACTER_KINDS = 3

/** Counts characters as the server does: in code points, so an emoji counts once. */
function characterCount(value: string): number {
  return [...value].length
}

/**
 * Every rule [password] breaks, in the server's order. A preview for live feedback only: the server
 * checks again and decides.
 */
export function unmetPasswordRules(
  password: string,
  email: string | null | undefined,
  policy: PasswordPolicy,
): PasswordRuleKey[] {
  const length = characterCount(password)
  const unmet: PasswordRuleKey[] = []
  if (length < policy.minLength) unmet.push('too_short')
  if (length > policy.maxLength) unmet.push('too_long')
  if (email && password.toLowerCase() === email.toLowerCase()) unmet.push('equals_email')
  return unmet
}

/** How a rule reads in the checklist shown before and while typing. */
export function passwordRuleLabel(rule: PasswordRuleKey, policy: PasswordPolicy): string {
  switch (rule) {
    case 'too_short': return `Au moins ${policy.minLength} caractères`
    case 'too_long': return `Au plus ${policy.maxLength} caractères`
    case 'equals_email': return 'Différent de votre adresse e-mail'
  }
}

/**
 * A rough indication, not an entropy estimate: below the minimum is weak, and length or variety beyond
 * it makes a password strong. It never blocks anything — only the rules do.
 */
export function passwordStrength(password: string, policy: PasswordPolicy): PasswordStrength {
  if (!password) return 'none'
  const length = characterCount(password)
  if (length < policy.minLength) return 'weak'

  const kinds = [/[a-z]/, /[A-Z]/, /\d/, /[^a-z\d]/i].filter(kind => kind.test(password)).length
  if (length >= policy.minLength + STRONG_LENGTH_MARGIN || kinds >= STRONG_CHARACTER_KINDS) return 'strong'
  return 'fair'
}

function joinInFrench(parts: string[]): string {
  if (parts.length <= 1) return parts.join('')
  return `${parts.slice(0, -1).join(', ')} et ${parts[parts.length - 1]}`
}

function isRuleKey(value: unknown): value is PasswordRuleKey {
  return value === 'too_short' || value === 'too_long' || value === 'equals_email'
}

// What each rule asks for, after "Le mot de passe doit", with the published lengths.
const RULE_REQUIREMENTS: Record<PasswordRuleKey, (policy: PasswordPolicy) => string> = {
  too_short: policy => `contenir au moins ${policy.minLength} caractères`,
  too_long: policy => `contenir au plus ${policy.maxLength} caractères`,
  equals_email: () => 'être différent de votre adresse e-mail',
}

// What is wrong, after "Le mot de passe", when the lengths are not known.
const RULE_PROBLEMS: Record<PasswordRuleKey, string> = {
  too_short: 'est trop court',
  too_long: 'est trop long',
  equals_email: 'ne doit pas être votre adresse e-mail',
}

/**
 * The sentence stating [rules], for display under the password field. Uses the published lengths when
 * [policy] is known, and plain wording otherwise.
 */
export function describeUnmetRules(rules: PasswordRuleKey[], policy: PasswordPolicy | null): string {
  if (rules.length === 0) return 'Le mot de passe ne respecte pas les règles de sécurité.'
  if (policy) return `Le mot de passe doit ${joinInFrench(rules.map(rule => RULE_REQUIREMENTS[rule](policy)))}.`
  return `Le mot de passe ${joinInFrench(rules.map(rule => RULE_PROBLEMS[rule]))}.`
}

/**
 * The sentence to show under the password field when [password] breaks a rule, or `null` when it
 * satisfies them — or when the policy could not be loaded, in which case the server alone decides. A
 * preview of the server's check, to spare a round trip: the server checks again.
 */
export function passwordRuleProblem(
  password: string,
  email: string | null | undefined,
  policy: PasswordPolicy | null,
): string | null {
  if (!policy) return null
  const unmet = unmetPasswordRules(password, email, policy)
  return unmet.length > 0 ? describeUnmetRules(unmet, policy) : null
}

/**
 * The sentence to show under the password field when the server refused it, or `null` when [payload] is
 * not a password policy refusal.
 */
export function policyViolationMessage(payload: unknown, policy: PasswordPolicy | null): string | null {
  if (!payload || typeof payload !== 'object') return null
  const body = payload as { errorKey?: unknown, reasons?: unknown }
  if (body.errorKey !== PASSWORD_POLICY_ERROR_KEY) return null

  const reasons = Array.isArray(body.reasons) ? body.reasons.filter(isRuleKey) : []
  return describeUnmetRules(reasons, policy)
}
