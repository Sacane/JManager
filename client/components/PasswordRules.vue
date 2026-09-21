<script setup lang="ts">
import type { PasswordRuleKey, PasswordStrength } from '~/utils/passwordPolicy'
import { passwordRuleLabel, passwordStrength, unmetPasswordRules } from '~/utils/passwordPolicy'

interface Props {
  password: string
  /** The account's address, when known: the password may not be it. */
  email?: string | null
  /** Given on the change-password screen, where the new password must differ from it. */
  currentPassword?: string
  /** After a refused submission: unmet rules are then shown as errors, not merely as not yet met. */
  showErrors?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  email: null,
  currentPassword: undefined,
  showErrors: false,
})

type ChecklistKey = PasswordRuleKey | 'differs_from_current'

interface ChecklistItem {
  key: ChecklistKey
  label: string
  met: boolean
}

const { policy, load } = usePasswordPolicy()

onMounted(() => {
  load()
})

const checklist = computed<ChecklistItem[]>(() => {
  const published = policy.value
  if (!published) return []

  const unmet = unmetPasswordRules(props.password, props.email, published)
  const items: ChecklistItem[] = published.rules
    // The field's maxlength already stops at the maximum; the address rule needs an address.
    .filter(rule => rule !== 'too_long' && (rule !== 'equals_email' || !!props.email))
    .map(rule => ({ key: rule, label: passwordRuleLabel(rule, published), met: !unmet.includes(rule) }))

  if (props.currentPassword !== undefined) {
    items.push({
      key: 'differs_from_current',
      label: 'Différent de votre mot de passe actuel',
      met: props.password.length > 0 && props.password !== props.currentPassword,
    })
  }
  return items
})

const strength = computed<PasswordStrength>(() =>
  policy.value ? passwordStrength(props.password, policy.value) : 'none',
)

const STRENGTH_LABELS: Record<Exclude<PasswordStrength, 'none'>, string> = {
  weak: 'faible',
  fair: 'correcte',
  strong: 'élevée',
}

const STRENGTH_SEGMENTS: Record<PasswordStrength, number> = { none: 0, weak: 1, fair: 2, strong: 3 }

const strengthLabel = computed(() => (strength.value === 'none' ? '' : STRENGTH_LABELS[strength.value]))
const filledSegments = computed(() => STRENGTH_SEGMENTS[strength.value])
</script>

<template>
  <div v-if="policy" class="password-rules" data-test="password-rules">
    <ul class="password-rules__list" aria-label="Règles du mot de passe">
      <li
        v-for="item in checklist"
        :key="item.key"
        class="password-rule"
        :class="{ 'password-rule--met': item.met, 'password-rule--unmet': props.showErrors && !item.met }"
        :data-test="`rule-${item.key}`"
        :data-met="String(item.met)"
      >
        <i :class="item.met ? 'pi pi-check-circle' : 'pi pi-circle'" aria-hidden="true" />
        <span>{{ item.label }}</span>
        <span class="sr-only">{{ item.met ? '(respectée)' : '(non respectée)' }}</span>
      </li>
    </ul>

    <div
      v-if="strength !== 'none'"
      class="password-strength"
      :class="`password-strength--${strength}`"
      data-test="password-strength"
      :data-level="strength"
    >
      <div class="password-strength__bar" aria-hidden="true">
        <span
          v-for="segment in 3"
          :key="segment"
          class="password-strength__segment"
          :class="{ 'password-strength__segment--filled': segment <= filledSegments }"
        />
      </div>
      <span class="text-note">Force : {{ strengthLabel }}</span>
    </div>
  </div>
</template>

<style scoped>
.password-rules {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.password-rules__list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-rule {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.password-rule i {
  font-size: 0.8rem;
}

.password-rule--met {
  color: var(--success);
}

.password-rule--unmet {
  color: var(--danger);
  font-weight: 600;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.password-strength__bar {
  display: flex;
  gap: 0.25rem;
  flex: 1;
  max-width: 9rem;
}

.password-strength__segment {
  flex: 1;
  height: 0.3rem;
  border-radius: 999px;
  background-color: var(--border-color);
  transition: background-color 0.2s ease;
}

.password-strength--weak .password-strength__segment--filled {
  background-color: var(--danger);
}

.password-strength--fair .password-strength__segment--filled {
  background-color: var(--warning);
}

.password-strength--strong .password-strength__segment--filled {
  background-color: var(--success);
}

@media (prefers-reduced-motion: reduce) {
  .password-rule,
  .password-strength__segment {
    transition: none;
  }
}
</style>
