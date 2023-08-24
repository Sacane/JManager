<script setup lang="ts">

</script>

<template>
  <div h-screen w-screen class="overflow-auto">
    <Header />
    <!-- ------------------------------ -->
    <div class=" flex-row ">
      <Sidebar class="mr5" />
      <div class="wscreen hscreen m-2">
        <slot />
      </div>
    </div>
    <footer class="bottom-0 w-full text-center">
      @copyright 2023
    </footer>
  </div>
</template>
