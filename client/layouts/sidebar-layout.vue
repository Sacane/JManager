<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row flex-gap-3">
    <!-- ------------------------------ -->
    <Sidebar class="mr5" />
    <div class="w-full h-full flex flex-col justify-center items-center">
      <slot />
    </div>
  </div>
</template>

<style scoped>

</style>
