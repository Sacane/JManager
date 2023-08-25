<script setup lang="ts">

</script>

<template>
  <div h-full w-auto class="overflow-auto flex-row">
    <!-- ------------------------------ -->
    <Sidebar class="mr5 h-screen" />
    <div flex-col w-screen>
      <div class="m-2 flex-row justify-center">
        <slot />
      </div>
      <footer class="bottom-0 text-center">
        @copyright 2023
      </footer>
    </div>
  </div>
</template>
