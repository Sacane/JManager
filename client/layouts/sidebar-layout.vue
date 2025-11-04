<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="flex min-h-screen w-full bg-gray-100">
    <AppSidebar />
    <main class="flex-1 min-h-screen w-full overflow-x-hidden">
      <slot />
    </main>
  </div>
</template>
