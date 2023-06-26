<script setup lang="ts">

</script>

<template>
  <Header />
  <!-- ------------------------------ -->
  <div class=" flex-row h-100%">
    <Sidebar class="mr5" />
    <div class="wfull hfull">
      <slot />
    </div>
  </div>
  <footer class="bottom-0 w-full text-center">
    @copyright 2023
  </footer>
</template>
