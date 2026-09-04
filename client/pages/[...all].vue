<script setup lang="ts">
definePageMeta({
  layout: 'centercard',
})

const { isAuthenticated } = useAuth()

const exit = computed(() =>
  isAuthenticated.value
    ? { to: '/', label: 'Retour au tableau de bord' }
    : { to: '/login', label: 'Aller à la page de connexion' },
)
</script>

<template>
  <div class="flex flex-col gap-6 text-center">
    <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[var(--bg-tertiary)] mx-auto mb-2">
      <i class="pi pi-compass text-[var(--primary)] text-2xl" aria-hidden="true" />
    </div>

    <div class="flex flex-col gap-2">
      <span class="text-caption">Erreur 404</span>
      <h1 class="heading-2">
        Cette page n'existe pas
      </h1>
      <p class="body-base max-w-sm mx-auto">
        L'adresse demandée est introuvable. Elle a peut-être été renommée, ou le lien que vous
        avez suivi est périmé.
      </p>
    </div>

    <NuxtLink :to="exit.to" class="btn-primary w-full text-center" data-test="not-found-exit-link">
      {{ exit.label }}
    </NuxtLink>
  </div>
</template>
