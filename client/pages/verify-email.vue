<script setup lang="ts">
definePageMeta({
  layout: 'centercard',
})

const route = useRoute()
const { verifyEmail, isVerifying, verifyResult, resendVerificationEmail, isResending, isResendOnCooldown, resendCooldown } = useEmailVerification()
const { emailVerified } = useConsent()
const toast = useJToast()

const token = computed(() => {
  const raw = route.query.token
  return typeof raw === 'string' ? raw : null
})

onMounted(async () => {
  if (!token.value) return
  const result = await verifyEmail(token.value)
  if (result === 'success') {
    // Refresh the cached verification state so the sidebar badge disappears.
    emailVerified.value = true
  }
})

async function handleResend() {
  await resendVerificationEmail(
    () => toast.success('E-mail de vérification envoyé. Vérifiez votre boîte de réception.'),
    () => toast.error('Impossible d\'envoyer l\'e-mail de vérification.'),
  )
}
</script>

<template>
  <div class="flex flex-col gap-6 text-center">
    <!-- Loading -->
    <template v-if="isVerifying || (!verifyResult && token)">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--primary)] mx-auto mb-2">
        <i class="pi pi-spin pi-spinner text-white text-2xl" aria-hidden="true" />
      </div>
      <p class="body-base">
        Vérification en cours…
      </p>
    </template>

    <!-- No token provided -->
    <template v-else-if="!token">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20 mx-auto mb-2">
        <i class="pi pi-times-circle text-red-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Lien invalide
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien de vérification est invalide. Vérifiez que vous avez copié l'URL complète.
      </p>
      <NuxtLink to="/" class="btn-primary w-full text-center">
        Accéder à l'application
      </NuxtLink>
    </template>

    <!-- Success -->
    <template v-else-if="verifyResult === 'success'">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/20 mx-auto mb-2">
        <i class="pi pi-check-circle text-green-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        E-mail vérifié !
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Votre adresse e-mail a été vérifiée avec succès.
      </p>
      <NuxtLink to="/" class="btn-primary w-full text-center" data-test="go-to-app-link">
        Accéder à l'application
      </NuxtLink>
    </template>

    <!-- Expired token -->
    <template v-else-if="verifyResult === 'expired'">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-amber-100 dark:bg-amber-900/20 mx-auto mb-2">
        <i class="pi pi-clock text-amber-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Lien expiré
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien de vérification a expiré. Vous pouvez en demander un nouveau.
      </p>
      <button
        class="btn-primary w-full"
        data-test="expired-resend-btn"
        :disabled="isResending || isResendOnCooldown"
        @click="handleResend"
      >
        <span v-if="isResending">Envoi en cours…</span>
        <span v-else-if="isResendOnCooldown">Renvoyer dans {{ resendCooldown }}s</span>
        <span v-else>Renvoyer un e-mail de vérification</span>
      </button>
      <NuxtLink to="/login" class="btn-outline-primary w-full text-center">
        Se connecter
      </NuxtLink>
    </template>

    <!-- Unknown / not found -->
    <template v-else>
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/20 mx-auto mb-2">
        <i class="pi pi-times-circle text-red-500 text-2xl" aria-hidden="true" />
      </div>
      <h1 class="heading-2">
        Lien invalide
      </h1>
      <p class="body-base max-w-sm mx-auto">
        Ce lien de vérification n'est pas reconnu. Il a peut-être déjà été utilisé.
      </p>
      <NuxtLink to="/login" class="btn-primary w-full text-center">
        Se connecter
      </NuxtLink>
    </template>
  </div>
</template>
