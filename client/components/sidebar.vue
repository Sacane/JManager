<script setup lang="ts">
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'

const { isAuthenticated, logout, user } = useAuth()
</script>

<template>
  <div class="hidden lg:flex flex-col border-r w-10% h-screen text-center justify-between content position-fixed">
    <div class="flex flex-col">
      <NuxtLink to="/" class="title decoration-none">
        <img src="@/public/favicon.ico" alt="icon" class="w-35px"> JManager
      </NuxtLink>
      <div class="mt5 flex flex-col flex-gap-4">
        <NuxtLink
          to="/" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white"
        >
          <i class="pi pi-home text-25px" />
          Accueil
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/dashboard" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white">
          <i class="pi pi-tag text-25px" /> Tableau de bord
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white">
          <i class="pi pi-wallet text-25px" /> Mes livrets
        </NuxtLink>
        <NuxtLink v-if="isAuthenticated" to="/tag" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white">
          <i class="pi pi-tag text-25px" /> Mes tags
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
.content{
  background-color: var(--primary);
  border-right: 1px solid #fff;
  @media (max-width: 770px) {
    width: 20%;
  }
}
.title {
  color: #fff;
  text-align: center;
  margin-top: 10px;
}

.section {
  color: #fff;
  font-size: 23px;
  background-color: var(--primary-background);

  &:hover{
    opacity: 0.6;
  }
  @media (min-width: 1201px) {
    i {
      align-self: flex-start;
      justify-self: flex-start;
    }
  }
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
.bg-primary-color-white{
  background-color: white;
  color: var(--primary);
}
</style>
