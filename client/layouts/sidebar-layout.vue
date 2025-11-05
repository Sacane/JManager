<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="flex min-h-screen w-full" style="background-color: var(--bg-primary);">
    <AppSidebar />
    <main class="flex-1 min-h-screen w-full overflow-x-hidden" style="background-color: var(--bg-primary);">
      <slot />
    </main>
  </div>
</template>
