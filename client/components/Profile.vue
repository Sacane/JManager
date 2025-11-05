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
  <div class="flex items-center justify-center flex-col gap-2.5 relative">
    <div class="flex flex-row gap-2 items-center">
      <div class="w-12.5 h-12.5 rounded-full bg-white flex items-center justify-center text-purple-600 text-3xl font-bold cursor-pointer" @click="toggleDropdown">
        {{ username.charAt(0) }}
      </div>
      <div class="flex flex-col gap-1">
        <p class="m-0 text-white font-medium">
          {{ username }}
        </p>
        <p class="text-xs text-white/80 m-0">
          {{ isAdmin ? 'Administrateur' : 'Utilisateur' }}
        </p>
      </div>
    </div>
    <Button class="btn-primary" @click="handleLogout">
      Se déconnecter
    </Button>
  </div>
</template>
