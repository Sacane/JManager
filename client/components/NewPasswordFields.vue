<script setup lang="ts">
// Imported rather than auto-resolved: page specs render these for real, and Vue Test Utils stubs
// components resolved by name inside a child, which would hide the fields and their errors.
import FieldError from './FieldError.vue'
import PasswordField from './PasswordField.vue'
import PasswordRules from './PasswordRules.vue'

/**
 * The two fields of a new password — the password with its rules, then its confirmation — each with its
 * inline error. Shared by every screen where a password is chosen without the current one.
 */
interface Props {
  /** Shown under the new password, where the checklist is: the password rules. */
  newPasswordError?: string | null
  /** Shown under the confirmation: missing fields or a mismatch. */
  confirmPasswordError?: string | null
  /** The account address, when known, so the checklist can tell the password must differ from it. */
  email?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  newPasswordError: null,
  confirmPasswordError: null,
  email: null,
})

const newPassword = defineModel<string>('newPassword', { required: true })
const confirmPassword = defineModel<string>('confirmPassword', { required: true })
</script>

<template>
  <div class="flex flex-col gap-4">
    <div class="flex flex-col gap-1">
      <label for="new-password" class="text-label">Nouveau mot de passe</label>
      <PasswordField
        id="new-password"
        v-model="newPassword"
        placeholder="Nouveau mot de passe"
        :maxlength="100"
        autocomplete="new-password"
        data-test="new-password-input"
      >
        <PasswordRules :password="newPassword" :email="props.email" :show-errors="!!props.newPasswordError" />
      </PasswordField>
      <FieldError data-test="new-password-error" :message="props.newPasswordError" />
    </div>

    <div class="flex flex-col gap-1">
      <label for="confirm-password" class="text-label">Confirmer le mot de passe</label>
      <PasswordField
        id="confirm-password"
        v-model="confirmPassword"
        placeholder="Confirmer le mot de passe"
        :maxlength="100"
        autocomplete="new-password"
        data-test="confirm-password-input"
      />
      <FieldError data-test="confirm-password-error" :message="props.confirmPasswordError" />
    </div>
  </div>
</template>
