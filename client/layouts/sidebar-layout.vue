<script setup lang="ts">

</script>

<template>
  <Header />
  <!-- ------------------------------ -->
  <div class=" flex-row h-screen w-screen">
    <Sidebar class="mr5" />
    <div class="wscreen hscreen m-2">
      <slot />
    </div>
  </div>
  <footer class="bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
