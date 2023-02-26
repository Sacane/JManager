<script setup lang="ts">
import useAuth from '@/composables/useAuth'
const { user, isAuthenticated } = useAuth()
</script>

<template>
  <header class="bg-gray-200 py-4 border-b">
    <div class="flex-row justify-between ml-2xl mr-2xl">
      <NuxtLink to="/" class="text-2xl font-bold">
        JManager
      </NuxtLink>
      <div class="flex-row">
        <div v-if="isAuthenticated">
          Bienvenue {{ user?.name }}
        </div>
        <button v-else class="icon-btn text-3 text-4">
          <div i="tabler-login" />
          Se connecter
        </button>
      </div>
    </div>
  </header>
  <div class="mt-3xl flex-row h-100%">
    <div>
      <Sidebar class="mr5" />
    </div>
    <slot />
  </div>
  <footer class="absolute bottom-0">
    @copyright 2023
  </footer>
</template>
