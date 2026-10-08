<script setup lang="ts">
import { passwordRuleProblem, policyViolationMessage } from '~/utils/passwordPolicy'

definePageMeta({
  layout: 'centercard',
})

type LinkState = 'checking' | 'valid' | 'expired' | 'invalid' | 'unavailable'

const { isConfirming, checkLink, confirmReset } = usePasswordReset()
const { policy: passwordPolicy } = usePasswordPolicy()
const toast = useJToast()

const linkState = ref<LinkState>('checking')
// Lives only here once taken off the address bar.
let token: string | null = null

const newPassword = ref('')
const confirmPassword = ref('')
/** The password rules, shown under the new password where the checklist is. */
const newPasswordError = ref<string | null>(null)
/** A mismatch, shown under the confirmation. */
const confirmPasswordError = ref<string | null>(null)
/** Failures attributable to no field. */
const formError = ref<string | null>(null)

// Synchronous, so an error disappears with the keystroke that may fix it.
watch(newPassword, () => {
  newPasswordError.value = null
  formError.value = null
}, { flush: 'sync' })
watch(confirmPassword, () => {
  if (newPassword.value === confirmPassword.value) confirmPasswordError.value = null
  formError.value = null
}, { flush: 'sync' })

/**
 * Takes the token out of the fragment, then out of the address bar and its history entry, so the page
 * can be left open, bookmarked or shared without carrying it. The router's history state is kept.
 */
function takeTokenFromAddressBar(): string | null {
  const fragment = new URLSearchParams(window.location.hash.slice(1))
  const found = fragment.get('token')
  window.history.replaceState(window.history.state, '', `${window.location.pathname}${window.location.search}`)
  return found || null
}

async function checkTheLink() {
  if (!token) {
    linkState.value = 'invalid'
    return
  }
  linkState.value = 'checking'
  linkState.value = await checkLink(token)
}

onMounted(async () => {
  token = takeTokenFromAddressBar()
  await checkTheLink()
})

function validate(): boolean {
  if (!newPassword.value || !confirmPassword.value) {
    confirmPasswordError.value = 'Saisissez puis confirmez votre nouveau mot de passe.'
    return false
  }
  if (newPassword.value !== confirmPassword.value) {
    confirmPasswordError.value = 'Les mots de passe ne correspondent pas.'
    return false
  }
  // The address rule is left to the server, which knows the address.
  newPasswordError.value = passwordRuleProblem(newPassword.value, null, passwordPolicy.value)
  return !newPasswordError.value
}

async function submit() {
  if (!token || !validate()) return

  const outcome = await confirmReset(token, newPassword.value, confirmPassword.value)
  switch (outcome.kind) {
    case 'reset':
      toast.success('Votre mot de passe a été modifié. Connectez-vous avec votre nouveau mot de passe.')
      navigateTo('/login')
      break
    case 'expired':
    case 'invalid':
      linkState.value = outcome.kind
      break
    case 'refused':
      newPasswordError.value = policyViolationMessage(outcome.payload, passwordPolicy.value)
      if (!newPasswordError.value) formError.value = 'Ce mot de passe n\'a pas été accepté. Vérifiez les deux champs.'
      break
    case 'rate_limited':
      formError.value = 'Trop de tentatives. Patientez quelques minutes avant de réessayer.'
      break
    default:
      toast.error('Le mot de passe n\'a pas pu être modifié. Veuillez réessayer.')
  }
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <!-- Checking the link -->
    <div v-if="linkState === 'checking'" class="text-center flex flex-col gap-4" data-test="reset-checking" role="status">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mx-auto">
        <i class="pi pi-spin pi-spinner text-white text-2xl" aria-hidden="true" />
      </div>
      <p class="body-base">
        Vérification du lien…
      </p>
    </div>

    <!-- New password -->
    <template v-else-if="linkState === 'valid'">
      <div class="text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mb-4">
          <i class="pi pi-lock text-white text-2xl" aria-hidden="true" />
        </div>
        <h1 class="heading-2 mb-2">
          Nouveau mot de passe
        </h1>
        <p class="body-base max-w-sm mx-auto">
          Choisissez le mot de passe que vous utiliserez désormais pour vous connecter.
        </p>
      </div>

      <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
        <NewPasswordFields
          v-model:new-password="newPassword"
          v-model:confirm-password="confirmPassword"
          :new-password-error="newPasswordError"
          :confirm-password-error="confirmPasswordError"
        />

        <FieldError data-test="reset-form-error" :message="formError" />

        <Button
          type="submit"
          label="Enregistrer le mot de passe"
          class="w-full"
          size="large"
          :loading="isConfirming"
          :disabled="isConfirming"
        />
      </form>
    </template>

    <!-- Expired: the link only lasts 30 minutes -->
    <div v-else-if="linkState === 'expired'" class="text-center flex flex-col gap-4" data-test="reset-link-expired">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-amber-100 dark:bg-amber-900/20 mx-auto">
        <i class="pi pi-clock text-amber-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Lien expiré
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien de réinitialisation a expiré : il n'est valable que 30 minutes. Demandez-en un nouveau.
      </p>
      <NuxtLink to="/forgot-password" class="btn-primary w-full text-center">
        Recevoir un nouveau lien
      </NuxtLink>
    </div>

    <!-- Unknown, already used or replaced -->
    <div v-else-if="linkState === 'invalid'" class="text-center flex flex-col gap-4" data-test="reset-link-invalid">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20 mx-auto">
        <i class="pi pi-times-circle text-red-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Lien invalide
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien de réinitialisation n'est plus valable : il a peut-être déjà servi, ou un lien plus récent
        l'a remplacé.
      </p>
      <NuxtLink to="/forgot-password" class="btn-primary w-full text-center">
        Recevoir un nouveau lien
      </NuxtLink>
    </div>

    <!-- The server could not answer: the link may still be good -->
    <div v-else class="text-center flex flex-col gap-4" data-test="reset-link-unavailable">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-amber-100 dark:bg-amber-900/20 mx-auto">
        <i class="pi pi-exclamation-triangle text-amber-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Vérification impossible
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien n'a pas pu être vérifié pour le moment. Patientez quelques minutes, puis réessayez.
      </p>
      <button type="button" class="btn-primary w-full" data-test="reset-retry-check" @click="checkTheLink">
        Réessayer
      </button>
    </div>

    <NuxtLink to="/login" class="text-center body-sm text-[var(--primary)] hover:underline">
      Retour à la connexion
    </NuxtLink>
  </div>
</template>
