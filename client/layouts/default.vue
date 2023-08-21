<script setup lang="ts">
</script>

<template>
  <Header />
  <!-- ------------------------------ -->

  <div class="w-screen h-screen">
    <slot />
  </div>

  <footer class="bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
