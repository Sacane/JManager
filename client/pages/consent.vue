<script setup lang="ts">
definePageMeta({
  layout: 'centercard',
  middleware: ['auth'],
  // Consent gates access to the application: no way out until it is given.
  allowBack: false,
})

const {
  tosAccepted,
  privacyAccepted,
  canSubmit,
  isSubmitting,
  tosVersion,
  checkConsentStatus,
  submitConsent,
} = useConsent()

const { success, errorAxios } = useJToast()

onMounted(async () => {
  const required = await checkConsentStatus()
  if (!required) {
    navigateTo('/')
  }
})

async function handleSubmit() {
  await submitConsent(
    () => {
      success('Votre consentement a été enregistré.')
      navigateTo('/')
    },
    (error) => {
      errorAxios(error, 'Erreur de consentement')
    },
  )
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- Header -->
    <div class="text-center">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mb-4">
        <i class="pi pi-shield text-white text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2 mb-2">
        Données personnelles
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Avant d'accéder à l'application, vous devez accepter nos conditions d'utilisation et notre politique de confidentialité.
      </p>
    </div>

    <!-- Consent checkboxes -->
    <div class="flex flex-col gap-5">
      <div class="flex items-start gap-3">
        <Checkbox
          v-model="tosAccepted"
          input-id="consent-tos"
          binary
          aria-label="J'accepte les Conditions Générales d'Utilisation"
        />
        <label for="consent-tos" class="body-base cursor-pointer leading-relaxed">
          J'accepte les
          <a
            href="/terms"
            target="_blank"
            rel="noopener noreferrer"
            class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline font-medium"
            aria-label="Lire les Conditions Générales d'Utilisation"
          >
            Conditions Générales d'Utilisation (v{{ tosVersion }})
          </a>
        </label>
      </div>

      <div class="flex items-start gap-3">
        <Checkbox
          v-model="privacyAccepted"
          input-id="consent-privacy"
          binary
          aria-label="J'accepte la Politique de confidentialité"
        />
        <label for="consent-privacy" class="body-base cursor-pointer leading-relaxed">
          J'accepte la
          <a
            href="/privacy"
            target="_blank"
            rel="noopener noreferrer"
            class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline font-medium"
            aria-label="Lire la Politique de confidentialité"
          >
            Politique de confidentialité
          </a>
        </label>
      </div>
    </div>

    <!-- GDPR informational note -->
    <p class="body-sm text-center">
      Ces acceptations sont enregistrées conformément au RGPD (Art. 7).
      Vous pouvez les consulter à tout moment depuis vos paramètres.
    </p>

    <!-- Submit -->
    <Button
      :label="isSubmitting ? '' : 'J\'accepte'"
      :loading="isSubmitting"
      :disabled="!canSubmit || isSubmitting"
      class="w-full"
      size="large"
      aria-label="Valider mon consentement et accéder à l'application"
      @click="handleSubmit"
    />
  </div>
</template>
