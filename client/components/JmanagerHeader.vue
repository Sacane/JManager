<script setup lang="ts">
const { user, logout } = useAuth()

const initial = computed(() => user.value?.username.charAt(0).toUpperCase())
</script>

<template>
  <div class="header-content">
    <div class="circle">
      {{ initial }}
    </div>
    <div>
      {{ user.username }}
    </div>
  </div>
</template>

<style scoped>
.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  gap: 10px;
}
.circle {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  background-color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 30px;
  font-weight: bold;
}
</style>
