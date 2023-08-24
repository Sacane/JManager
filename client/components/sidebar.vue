import useAuth from '../composables/useAuth';
<script setup lang="ts">
import useAuth from '@/composables/useAuth'
import 'primeicons/primeicons.css';
const { isAuthenticated } = useAuth()
</script>

<template>
  <div class="flex-column border-r h-100% decoration-none w-15% text-center">
    <div class="mt5">
      <NuxtLink to="/" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF color-#fff">
        <i class="pi pi-home text-20px"></i>
        Accueil
      </NuxtLink>
      <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF">
        <i class="pi pi-user text-20px"/> Mes comptes
      </NuxtLink>
      <!--<NuxtLink v-if="isAuthenticated" to="/transaction" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF">
        <i class="pi pi-money-bill text-20px"/> Mes transactions
      </NuxtLink>-->
    </div>
  </div>
</template>
