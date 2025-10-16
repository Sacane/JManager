<script setup lang="ts">
import AppSidebar from '~/components/app-sidebar.vue'
</script>

<template>
  <div class="layout-wrapper">
    <AppSidebar class="sidebar" />
    <div class="main-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.layout-wrapper {
  display: flex;
  min-height: 100vh;
  width: 100%;
  position: relative;
}

.sidebar {
  position: sticky;
  top: 0;
  left: 0;
  width: 230px;
  height: 100vh;
  overflow-y: auto;
  flex-shrink: 0;
  z-index: 100;
}

.main-content {
  flex: 1;
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
}

/* Scrollbar pour la sidebar */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar::-webkit-scrollbar-thumb {
  background: rgba(130, 42, 204, 0.3);
  border-radius: 10px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(130, 42, 204, 0.5);
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -230px;
    transition: left 0.3s ease;
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  }

  .sidebar.mobile-open {
    left: 0;
  }

  .main-content {
    width: 100%;
    margin-left: 0;
  }
}
</style>
