<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row w-screen h-screen">
    <!-- ------------------------------ -->
    <Sidebar class="sidebar" />
    <div class="content lg:(w-full h-screen flex flex-col)">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 15%;
  position: fixed;
}

.content {
  width: 85%;
  margin-left: 15%;
}
@media (max-width: 768px) {
  .content {
    width: 100%;
    margin-left: 0;
  }
  .sidebar {
    width: 0;
  }
}
</style>
