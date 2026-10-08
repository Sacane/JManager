<script setup lang="ts">
import authMiddleware from '~/middleware/auth'

definePageMeta({
  layout: 'centercard',
  middleware: [authMiddleware],
  // The new password is mandatory: no way out until it is set.
  allowBack: false,
})

const {
  newPassword,
  confirmPassword,
  passwordError,
  newPasswordError,
  isSubmitting,
  submit,
} = useForceChangePassword()
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="text-center">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mb-4">
        <i class="pi pi-lock text-white text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2 mb-2">
        Changement de mot de passe requis
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Pour sécuriser votre compte, veuillez définir un nouveau mot de passe.
      </p>
    </div>

    <NewPasswordFields
      v-model:new-password="newPassword"
      v-model:confirm-password="confirmPassword"
      :new-password-error="newPasswordError"
      :confirm-password-error="passwordError"
    />

    <Button
      label="Changer mon mot de passe"
      :loading="isSubmitting"
      :disabled="isSubmitting"
      class="w-full"
      size="large"
      data-test="submit-btn"
      aria-label="Changer mon mot de passe"
      @click="submit"
    />
  </div>
</template>
