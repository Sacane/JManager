<script setup lang="ts">
import useAuth from '@/composables/useAuth'
const { user, isAuthenticated, logout } = useAuth()
</script>

<template>
  <!-- TODO Make this part a component header -->
  <header class="bg-gray-200 py2 border-b">
    <div class="flex-row justify-between ml-2 mr-2xl">
      <NuxtLink to="/" class="text-2xl font-bold">
        JManager
      </NuxtLink>
      <h1 v-if="isAuthenticated">
        Bonjour {{ user?.username }} !
      </h1>
      <div class="flex-row">
        <button v-if="isAuthenticated" class="icon-btn text-3 text4" @click="logout()">
          <div i="tabler-logout" />
          Se deconnecter
        </button>
        <NuxtLink v-else to="/login" class="icon-btn text-3 text-4">
          <div i="tabler-login" />
          Se connecter
        </NuxtLink>
      </div>
    </div>
  </header>
  <!-- ------------------------------ -->
  <div class=" flex-row h-100%">
    <div v-if="isAuthenticated">
      <Sidebar class="mr5" />
    </div>
    <div class="wfull hfull">
      <slot />
    </div>
  </div>
  <footer class="bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
