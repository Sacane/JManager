<script setup lang="ts">
import useAuth from '@/composables/useAuth'
const { isAuthenticated } = useAuth()
</script>

<template>
  <Header />
  <!-- ------------------------------ -->
  <div class=" flex-row h-100%">
    <Sidebar v-if="isAuthenticated" class="mr5" />
    <div class="wfull hfull">
      <slot />
    </div>
  </div>
  <footer class="bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
