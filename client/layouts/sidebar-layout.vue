<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="flex h-[100dvh] w-full overflow-hidden" style="background-color: var(--bg-primary);">
    <AppSidebar />
    <main class="flex-1 h-full overflow-y-auto overflow-x-hidden overscroll-none" style="background-color: var(--bg-primary);">
      <slot />
    </main>
  </div>
</template>
