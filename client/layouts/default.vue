<script setup lang="ts">
import useAuth from '@/composables/useAuth'
const { user, isAuthenticated } = useAuth()
onMounted(() => {
  console.table(user.value)
})
</script>

<template>
  <header class="bg-gray-200 py2 border-b">
    <div class="flex-row justify-between ml-2 mr-2xl">
      <NuxtLink to="/" class="text-2xl font-bold">
        JManager
      </NuxtLink>
      <div v-if="isAuthenticated">
        Bienvenue {{ user?.username }}
      </div>
      <div class="flex-row">
        <NuxtLink v-if="isAuthenticated" to="/login" class="icon-btn text-3 text4">
          <div i="tabler-logout">
            Se deconnecter
          </div>
        </NuxtLink>
        <NuxtLink v-else to="/login" class="icon-btn text-3 text-4">
          <div i="tabler-login" />
          Se connecter
        </NuxtLink>
      </div>
    </div>
  </header>
  <div class=" flex-row h-100%">
    <div>
      <Sidebar class="mr5" />
    </div>
    <div class="wfull hfull">
      <slot />
    </div>
  </div>
  <footer class="absolute bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
