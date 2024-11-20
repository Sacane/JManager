<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row w-screen h-screen">
    <!-- ------------------------------ -->
    <Sidebar class="sidebar" />
    <div class="lg:(w-full h-screen flex flex-col ml-13%)">
      <slot />
    </div>
  </div>
</template>

<style scoped>
</style>
