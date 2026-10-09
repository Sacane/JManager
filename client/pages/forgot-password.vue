<script setup lang="ts">
definePageMeta({
  layout: 'centercard',
})

const { isRequesting, requestReset, takeRememberedEmail } = usePasswordReset()
const toast = useJToast()

const email = ref(takeRememberedEmail())
const emailError = ref<string | null>(null)
// The address the link was requested for, once sent: the page then only confirms.
const sentTo = ref<string | null>(null)

watch(email, () => {
  emailError.value = null
})

async function submit() {
  const address = email.value.trim()
  if (!address) {
    emailError.value = 'Saisissez votre adresse e-mail.'
    return
  }

  const outcome = await requestReset(address)
  if (outcome === 'sent') {
    sentTo.value = address
  } else if (outcome === 'rate_limited') {
    emailError.value = 'Trop de demandes. Patientez quelques minutes avant de réessayer.'
  } else {
    toast.error('La demande n\'a pas pu être envoyée. Veuillez réessayer.')
  }
}

function tryAnotherAddress() {
  sentTo.value = null
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- Sent: the same answer whether or not an account matches -->
    <template v-if="sentTo">
      <div class="text-center flex flex-col gap-4" data-test="reset-request-sent" role="status">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/20 mx-auto">
          <i class="pi pi-envelope text-green-500 text-2xl" aria-hidden="true" />
        </div>
        <h1 class="heading-2">
          Vérifiez votre boîte de réception
        </h1>
        <!-- break-words: a long address must not push the card wider than a phone -->
        <p class="body-base max-w-sm mx-auto break-words">
          Si un compte correspond à {{ sentTo }}, un e-mail vient de lui être envoyé avec un lien pour
          choisir un nouveau mot de passe. Ce lien est valable 30 minutes.
        </p>
        <p class="body-sm max-w-sm mx-auto">
          Rien reçu ? Pensez à vérifier vos courriers indésirables.
        </p>
      </div>
      <button type="button" class="btn-outline-primary w-full" data-test="reset-try-another" @click="tryAnotherAddress">
        Utiliser une autre adresse
      </button>
    </template>

    <!-- Request form -->
    <template v-else>
      <div class="text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mb-4">
          <i class="pi pi-key text-white text-2xl" aria-hidden="true" />
        </div>
        <h1 class="heading-2 mb-2">
          Mot de passe oublié
        </h1>
        <p class="body-base max-w-sm mx-auto">
          Indiquez l'adresse e-mail de votre compte : nous vous enverrons un lien pour choisir un nouveau
          mot de passe.
        </p>
      </div>

      <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
        <div class="flex flex-col gap-1">
          <label for="reset-email" class="text-label">Adresse e-mail</label>
          <InputText
            id="reset-email"
            v-model="email"
            type="email"
            class="w-full"
            placeholder="Entrez votre adresse e-mail"
            maxlength="255"
            autocomplete="email"
            data-test="reset-email-input"
          />
          <FieldError data-test="reset-email-error" :message="emailError" />
        </div>

        <Button
          type="submit"
          label="Envoyer le lien"
          class="w-full"
          size="large"
          :loading="isRequesting"
          :disabled="isRequesting"
        />
      </form>
    </template>

    <NuxtLink to="/login" class="text-center body-sm text-[var(--primary)] hover:underline">
      Retour à la connexion
    </NuxtLink>
  </div>
</template>
