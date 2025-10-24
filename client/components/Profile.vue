<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import useAuth from '@/composables/useAuth'

const { user, logout, isAdmin } = useAuth()

const username = computed(() => user.value?.username.charAt(0).toUpperCase().concat(user.value?.username.slice(1)) ?? '')
const showDropdown = ref(false)

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function handleLogout() {
  logout()
  showDropdown.value = false
}

function handleClickOutside(event: MouseEvent) {
  const dropdown = document.querySelector('.dropdown-menu')
  const circle = document.querySelector('.circle')
  if (dropdown && !dropdown.contains(event.target as Node) && !circle?.contains(event.target as Node)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="header-content">
    <div class="flex flex-row gap-2 items-center">
      <div class="circle" @click="toggleDropdown">
        {{ username.charAt(0) }}
      </div>
      <div>
        <p>{{ username }}</p>
        <p class="role">
          {{ isAdmin ? 'Administrateur' : 'Utilisateur' }}
        </p>
      </div>
    </div>
    <Button class="btn-primary" @click="handleLogout">
      Se déconnecter
    </Button>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap');

.header-content {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  position: relative;
}

.circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  font-size: 30px;
  font-weight: bold;
  cursor: pointer;
}

.role {
  font-size: 12px;
  color: var(--text-color);
  font-family: 'Roboto', sans-serif;
  margin-top: -12px;
}

.dropdown-menu button {
  background: none;
  border: none;
  padding: 10px 20px;
  width: 100%;
  text-align: left;
  cursor: pointer;
  font-family: 'Roboto', sans-serif;
}

.dropdown-menu button:hover {
  background-color: #f0f0f0;
}
</style>
