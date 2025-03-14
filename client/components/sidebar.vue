<script setup lang="ts">
import { ref } from 'vue'
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'

const { isAuthenticated, logout, user } = useAuth()
const isSidebarOpen = ref(true)
function isMobile() {
  return window.innerWidth <= 768
}
function closeOnNavigateIfMobile() {
  if (isMobile()) {
    isSidebarOpen.value = false
  }
}
</script>

<template>
  <div>
    <button v-if="!isSidebarOpen" class="lg:hidden p-2 fixed top-2 left-2" @click="isSidebarOpen = !isSidebarOpen">
      <i class="pi pi-bars text-2xl" />
    </button>
    <div :class="{ 'sidebar-open': isSidebarOpen, 'sidebar-closed': !isSidebarOpen }" class="sidebar lg:flex flex-col border-r w-full lg:w-80 h-screen text-center justify-between content fixed lg:relative">
      <div class="flex flex-col">
        <NuxtLink to="/" class="title">
          <img src="@/public/favicon.ico" alt="icon" class="w-10 lg:w-10">
        </NuxtLink>
        <div class="mt-5 flex flex-col gap-4">
          <NuxtLink
            to="/" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white"
            @click="closeOnNavigateIfMobile()"
          >
            <i class="pi pi-home text-2xl lg:text-3xl" />
            Accueil
          </NuxtLink>
          <NuxtLink v-if="isAuthenticated" to="/dashboard" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-tag text-2xl lg:text-3xl" /> Tableau de bord
          </NuxtLink>
          <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-wallet text-2xl lg:text-3xl" /> Mes livrets
          </NuxtLink>
          <NuxtLink v-if="isAuthenticated" to="/tag" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-tag text-2xl lg:text-3xl" /> Mes tags
          </NuxtLink>
        </div>
      </div>

      <div class="m-2">
        <p class="mb-5">
          Connecté en tant que <b>{{ user?.username }}</b>
        </p>
        <div class="flex-row text-lg flex justify-center">
          <NuxtLink v-if="isAuthenticated" class="icon-btn mb-2" @click="logout()">
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
  </div>
</template>

<style scoped lang="scss">
.content {
  background-color: var(--primary);
  border-right: 1px solid #fff;
}
.open {
  z-index: 2000;
}
.close {
  visibility: hidden;
}
.title {
  color: #fff;
}

.section {
  color: #fff;
  font-size: 1.5rem;
  background-color: var(--primary-background);

  &:hover {
    opacity: 0.6;
  }
}

.sidebar {
  transition: transform 0.3s ease-in-out;
  z-index: 1000;
  width: 100%;
  height: 100%;
}

.sidebar-open {
  transform: translateX(0);
}

.sidebar-closed {
  transform: translateX(-200%);
  visibility: hidden;
  width: 0 !important;
}

.bg-primary-color-white {
  background-color: white;
  color: var(--primary);
}
</style>
