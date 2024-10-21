<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row">
    <!-- ------------------------------ -->
    <Sidebar class="sidebar" />
    <div class="w-full flex flex-col">
      <slot />
    </div>
  </div>
</template>

<style scoped>
</style>
