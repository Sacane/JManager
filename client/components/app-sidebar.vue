<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import Profile from '~/components/Profile.vue'
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'

const { isAuthenticated } = useAuth()
const isSidebarOpen = ref(false)
const isMobileView = ref(false)

function checkMobile() {
  isMobileView.value = window.innerWidth <= 768
  if (!isMobileView.value) {
    isSidebarOpen.value = true
  }
}

function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

function closeOnNavigateIfMobile() {
  if (isMobileView.value) {
    isSidebarOpen.value = false
  }
}

function handleClickOutside(event: MouseEvent) {
  if (isMobileView.value && isSidebarOpen.value) {
    const sidebar = document.querySelector('.sidebar')
    const toggleBtn = document.querySelector('.toggle-btn')
    if (sidebar && !sidebar.contains(event.target as Node) && !toggleBtn?.contains(event.target as Node)) {
      isSidebarOpen.value = false
    }
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div>
    <!-- Overlay pour mobile -->
    <Transition name="fade">
      <div v-if="isSidebarOpen && isMobileView" class="overlay" @click="isSidebarOpen = false" />
    </Transition>

    <!-- Bouton toggle pour mobile -->
    <button
      class="toggle-btn"
      :class="{ 'btn-hidden': isSidebarOpen && isMobileView }"
      @click="toggleSidebar"
    >
      <i class="pi pi-bars" />
    </button>

    <!-- Sidebar -->
    <aside
      :class="{
        'sidebar-open': isSidebarOpen,
        'sidebar-closed': !isSidebarOpen && isMobileView,
      }"
      class="sidebar"
    >
      <div class="sidebar-content">
        <!-- Header avec logo -->
        <div class="sidebar-header">
          <NuxtLink to="/" class="logo-container" @click="closeOnNavigateIfMobile()">
            <img src="@/public/favicon.ico" alt="icon" class="logo">
            <span class="logo-text">Mon App</span>
          </NuxtLink>

          <!-- Bouton fermer pour mobile -->
          <button v-if="isMobileView" class="close-btn" @click="isSidebarOpen = false">
            <i class="pi pi-times" />
          </button>
        </div>

        <!-- Navigation -->
        <nav class="sidebar-nav">
          <div v-if="isAuthenticated" class="nav-section">
            <NuxtLink
              to="/dashboard"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-home" />
              <span>Tableau de bord</span>
            </NuxtLink>

            <NuxtLink
              to="/account"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-wallet" />
              <span>Mes livrets</span>
            </NuxtLink>

            <NuxtLink
              to="/tag"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-tag" />
              <span>Mes tags</span>
            </NuxtLink>

            <NuxtLink
              to="/regular-transaction"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-refresh" />
              <span>Transactions régulières</span>
            </NuxtLink>
          </div>
        </nav>

        <!-- Footer avec profil -->
        <div class="sidebar-footer">
          <Profile />
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped lang="scss">
.overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.toggle-btn {
  position: fixed;
  top: 1rem;
  left: 1rem;
  z-index: 1001;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 12px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;

  i {
    font-size: 1.25rem;
  }

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  }

  &:active {
    transform: scale(0.95);
  }

  @media (min-width: 769px) {
    display: none;
  }
}

.btn-hidden {
  opacity: 0;
  pointer-events: none;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 280px;
  background: linear-gradient(180deg, var(--primary) 0%, var(--primary-2) 100%);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;

  @media (max-width: 768px) {
    width: 280px;
  }
}

.sidebar-open {
  transform: translateX(0);
}

.sidebar-closed {
  transform: translateX(-100%);

  @media (min-width: 769px) {
    transform: translateX(0);
  }
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.sidebar-header {
  padding: 1.5rem 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: white;
  padding: 0.25rem;
}

.logo-text {
  color: white;
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.close-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  &:active {
    transform: scale(0.95);
  }

  i {
    font-size: 1.1rem;
  }
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 1rem 0.75rem;

  /* Scrollbar personnalisée */
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.3);
  }
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem 1rem;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;

  i {
    font-size: 1.25rem;
    flex-shrink: 0;
    width: 24px;
    text-align: center;
  }

  span {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    width: 3px;
    background: white;
    transform: scaleY(0);
    transition: transform 0.2s ease;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: white;
    transform: translateX(2px);
  }

  &:active {
    transform: translateX(0) scale(0.98);
  }
}

.nav-item-active {
  background: white;
  color: var(--primary);
  font-weight: 600;

  &::before {
    transform: scaleY(1);
    background: var(--primary);
  }

  &:hover {
    background: white;
    color: var(--primary);
    transform: translateX(2px);
  }
}

.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 1rem 1.25rem;
  flex-shrink: 0;
  background: rgba(0, 0, 0, 0.05);
}

@media (min-width: 769px) {
  .sidebar {
    position: sticky;
    top: 0;
  }
}
</style>
