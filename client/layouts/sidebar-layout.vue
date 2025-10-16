<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="layout-wrapper">
    <AppSidebar />
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-wrapper {
  display: flex;
  min-height: 100vh;
  width: 100%;
  background: #f5f5f7;
}

.main-content {
  flex: 1;
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;

  @media (min-width: 769px) {
    margin-left: 0;
  }
}
</style>
