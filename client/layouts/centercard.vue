<script setup lang="ts">
const route = useRoute()

// Screens that gate access to the application — consent, forced password change — opt out by
// declaring `allowBack: false`. Showing a way out there invites an escape the middleware
// immediately undoes, which the user experiences as a redirect loop.
const allowBack = computed(() => route.meta?.allowBack !== false)
</script>

<template>
  <div class="h-screen">
    <NHeader />
    <div class="h-full flex-center flex-col gap-5">
      <div class="bg-white dark:bg-dark rounded-xl border-bluegray shadow-md border-1 border-opacity-25% p-10 w-100 sm:w-125 lg:w-170">
        <slot />
      </div>
    </div>
  </div>
  <div v-if="allowBack" class="absolute bottom-10 right-10">
    <NuxtLinkLocale to="/" class="icon-btn text-4xl" aria-label="Retour à l'application">
      <div i="tabler-arrow-left" />
    </NuxtLinkLocale>
  </div>
</template>
