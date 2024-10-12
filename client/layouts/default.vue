<script setup lang="ts">
</script>

<template>
  <div class="flex flex-col">
    <div class="w-full h-screen flex flex-col justify-center items-center">
      <slot />
      <footer class="bottom-0 w-full text-center">
        @copyright 2023
      </footer>
    </div>
  </div>
</template>
