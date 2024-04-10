<script setup lang="ts">
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'

const { isAuthenticated, logout, user } = useAuth()
</script>

<template>
  <div class="flex flex-col border-r decoration-none w-15% h-screen text-center justify-between content">
    <div class="flex flex-col">
      <NuxtLink to="/" class="title h-40px decoration-none">
        JManager
      </NuxtLink>
      <div class="mt5 flex flex-col">
        <NuxtLink to="/" class="hover:bg-gray-200 px-4 py-2 decoration-none" active-class="bg-gray-200">
          <i class="pi pi-home text-20px" />
          Accueil
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 px-4 py-2 decoration-none" active-class="bg-gray-200">
          <i class="pi pi-user text-20px" /> Mes comptes
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/tag" class="hover:bg-gray-200 px-4 py-2 decoration-none" active-class="bg-gray-200">
          <i class="pi pi-user text-20px" /> Mes tags
        </NuxtLink>
      </div>
    </div>

    <div class="m2">
      <p class="mb5">
        Connecté en tant que <b>{{ user?.username }}</b>
      </p>
      <div class="flex-row text-15px flex justify-center">
        <NuxtLink v-if="isAuthenticated" class="icon-btn mb2px" @click="logout()">
          <i class="pi pi-sign-out" />
          Se deconnecter
        </NuxtLink>
        <NuxtLink v-else to="/login" class="icon-btn">
          <div i="tabler-login" />
          Se connecter
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.title {
  color: var(--primary);
  text-align: center;
}

@media screen and (min-width: 1201px) {
  .title{
    font-size: 2rem;
  }
}

@media screen and (max-width: 768px) {
  .title{
    font-size: 1rem;
  }
}

@media screen and (min-width: 769px) and (max-width: 1200px) {
  .title{
    font-size: 1.5rem;
  }
}
</style>
