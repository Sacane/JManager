import useAuth from '../composables/useAuth';
<script setup lang="ts">
import useAuth from '@/composables/useAuth'
const { isAuthenticated } = useAuth()
</script>

<template>
  <div class="flex-column border-r h-100% mr3 decoration-none">
    <NuxtLink to="/" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF color-#fff">
      Accueil
    </NuxtLink>
    <NuxtLink v-if="isAuthenticated" to="/account" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF">
      Mes comptes
    </NuxtLink>
    <NuxtLink v-if="isAuthenticated" to="/transaction" class="hover:bg-gray-200 px-4 py-2" active-class="bg-#7F52FF">
      Mes transactions
    </NuxtLink>
  </div>
</template>
