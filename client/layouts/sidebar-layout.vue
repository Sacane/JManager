<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row">
    <!-- ------------------------------ -->
    <Sidebar class="sidebar" />
    <div class="w-full h-full flex flex-col justify-center items-center">
      <slot />
    </div>
  </div>
</template>

<style scoped>
</style>
