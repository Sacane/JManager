<script setup lang="ts">
import { ref } from 'vue'
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'
import Profile from '~/components/Profile.vue'

const { isAuthenticated } = useAuth()
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
      <div class="flex flex-col h-full">
        <NuxtLink to="/" class="title">
          <img src="@/public/favicon.ico" alt="icon" class="w-10 lg:w-10">
        </NuxtLink>
        <div class="mt-4 flex flex-col gap-2">
          <NuxtLink v-if="isAuthenticated" to="/dashboard" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-home text-20px" /> Tableau de bord
          </NuxtLink>
          <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-wallet text-20px" /> Mes livrets
          </NuxtLink>
          <NuxtLink v-if="isAuthenticated" to="/tag" class="hover:bg-gray-200 py-2 px-4 decoration-none section flex flex-row overflow-hidden gap-3" active-class="bg-primary-color-white" @click="closeOnNavigateIfMobile()">
            <i class="pi pi-tag text-20px" /> Mes tags
          </NuxtLink>
        </div>
        <div class="h-full flex flex-col justify-end mb-20">
          <Profile />
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
  font-size: 1.1rem;
  background-color: var(--primary-2);

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
