<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="flex h-[100dvh] w-full overflow-hidden" style="background-color: var(--bg-primary);">
    <AppSidebar />
    <main class="page-content flex-1 h-full overflow-y-auto overflow-x-hidden overscroll-none" style="background-color: var(--bg-primary);">
      <slot />
    </main>
  </div>
</template>

<style scoped>
/* The sidebar toggle button is `position: fixed` at the top-left below 769px
   (see components/app-sidebar.vue). Offsetting the scroll container here keeps every page
   clear of it, instead of each page having to re-declare its own margin. The breakpoint
   mirrors the one that hides the button, so the two never disagree. */
.page-content {
  padding-top: 4rem;
}

@media (min-width: 769px) {
  .page-content {
    padding-top: 0;
  }
}
</style>
