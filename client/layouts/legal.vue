<template>
  <div class="fixed inset-0 overflow-y-auto surface-page">
    <NHeader />
    <main class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-16">
      <slot />
    </main>
    <div class="fixed bottom-6 left-6">
      <NuxtLink
        to="/login"
        class="icon-btn text-2xl"
        aria-label="Retour à l'application"
      >
        <div i="tabler-arrow-left" />
      </NuxtLink>
    </div>
  </div>
</template>
