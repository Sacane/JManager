<script setup lang="ts">

</script>

<template>
  <div h-full wfull w-auto class="overflow-auto flex-row">
    <!-- ------------------------------ -->
    <Sidebar class="mr5" />
    <PToast/>
    <div flex-col w-screen justify-between hfull>
      <div class="m-2 flex-row justify-center hfull">
        <slot />
      </div>
      <footer class="bottom-0 text-center">
        @copyright 2023
      </footer>
    </div>
  </div>
</template>
