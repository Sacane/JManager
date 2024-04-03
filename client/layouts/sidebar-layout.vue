<script setup lang="ts">

</script>

<template>
  <div class="overflow-auto flex-row h-full w-full">
    <!-- ------------------------------ -->
    <Sidebar class="mr5" />
    <PToast/>
    <div class="flex-col w-screen justify-between h-full">
      <div class="m-2 h-full">
        <slot />
      </div>
      <footer class="bottom-0 text-center">
        @copyright 2023
      </footer>
    </div>
  </div>
</template>
