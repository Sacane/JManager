<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import useAuth from '@/composables/useAuth'

const { user, logout } = useAuth()

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
  if (username.value === '') {
    navigateTo('/login')
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="header-content">
    <div class="circle" @click="toggleDropdown">
      {{ username.charAt(0) }}
    </div>
    <div>
      <p>{{ username }}</p>
    </div>
    <transition name="dropdown">
      <div v-if="showDropdown" class="dropdown-menu">
        <button @click="handleLogout">
          Se déconnecter
        </button>
      </div>
    </transition>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap');

.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  gap: 10px;
  position: relative;
}

.circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 30px;
  font-weight: bold;
  cursor: pointer;
}

.role {
  font-size: 15px;
  color: var(--text-color);
  font-family: 'Roboto', sans-serif;
}

.dropdown-menu {
  position: absolute;
  top: 85px; /* Adjust this value to position the dropdown below the circle */
  right: 10px;
  background-color: white;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
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

.dropdown-enter-active, .dropdown-leave-active {
  transition: all 0.1s ease;
}

.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
