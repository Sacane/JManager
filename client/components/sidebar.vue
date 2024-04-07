import useAuth from '../composables/useAuth';
<script setup lang="ts">
import useAuth from '@/composables/useAuth'
import 'primeicons/primeicons.css';

const { isAuthenticated, logout, user } = useAuth()
const username = ref(isAuthenticated ? user.value?.username : '')
</script>

<template>
  <div class="flex-column border-r decoration-none w-15% h-screen text-center justify-between content">
    <div>
      <NuxtLink to="/" class="font-bold mb-2  h-full title">
        JManager
      </NuxtLink>
      <div class="mt5">
        <NuxtLink to="/" class="hover:bg-gray-200 px-4 py-2" active-class="bg-gray-200">
          <i class="pi pi-home text-20px"></i>
          Accueil
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 px-4 py-2" active-class="bg-gray-200">
          <i class="pi pi-user text-20px"/> Mes comptes
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/tag" class="hover:bg-gray-200 px-4 py-2" active-class="bg-gray-200">
          <i class="pi pi-user text-20px"/> Mes tags
        </NuxtLink>
      </div>
    </div>

    <div class="m2">
      <p class="mb5">Connecté en tant que <b>{{ user?.username }}</b></p>
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
  background-color: var(--primary);
  color: white;
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

.content{
  font-style: sans-serif;
}
</style>
