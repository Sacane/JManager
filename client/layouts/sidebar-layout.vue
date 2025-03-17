<script setup lang="ts">

</script>

<template>
  <div class="flex flex-row w-screen-h-screen">
    <Sidebar class="sidebar" />
    <div class="flex flex-col w-screen h-screen">
      <!-- ------------------------------ -->
      <JmanagerHeader class="header" />
      <div class="content lg:(w-full h-screen flex flex-col)">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 230px;
  position: fixed;
}

.content {
  width: 85%;
  margin-left: 230px;
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
.header {
  height: 100px;
  width: 100%;
  padding: 20px;
}
</style>
