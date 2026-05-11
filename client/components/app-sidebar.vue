<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import Profile from '~/components/Profile.vue'
import useAuth from '../composables/useAuth'
import 'primeicons/primeicons.css'

const { isAuthenticated, isAdmin } = useAuth()
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
    <Transition name="fade">
      <div v-if="isSidebarOpen && isMobileView" class="overlay" @click="isSidebarOpen = false" />
    </Transition>

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
        <div class="sidebar-header">
          <NuxtLink to="/" class="logo-container" @click="closeOnNavigateIfMobile()">
            <img src="@/public/favicon.ico" alt="icon" class="logo">
            <span class="logo-text">Jmanager</span>
          </NuxtLink>

          <div class="header-actions">
            <DarkModeToggle />
            <button v-if="isMobileView" class="close-btn" @click="isSidebarOpen = false">
              <i class="pi pi-times" />
            </button>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div v-if="isAuthenticated" class="nav-section">
            <NuxtLink
              to="/"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-home" />
              <span>Tableau de bord</span>
            </NuxtLink>

            <NuxtLink
              to="/booklet"
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

            <NuxtLink
              to="/settings"
              class="nav-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-cog" />
              <span>Paramètres</span>
            </NuxtLink>

            <NuxtLink
              v-if="isAdmin"
              to="/admin"
              class="nav-item admin-item"
              active-class="nav-item-active"
              @click="closeOnNavigateIfMobile()"
            >
              <i class="pi pi-shield" />
              <span>Administration</span>
            </NuxtLink>
          </div>
        </nav>

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

:global(.dark) & {
    background: #1e293b;
    border: 1px solid rgba(130, 42, 204, 0.4);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5), 0 0 10px rgba(130, 42, 204, 0.15);
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
  background: #190233;
  border-right: 2px solid #45058C;
  box-shadow: 4px 0 28px rgba(0, 0, 0, 0.4), 2px 0 0 rgba(69, 5, 140, 0.3);
  z-index: 1000;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              background 0.4s ease,
              box-shadow 0.4s ease,
              border-color 0.4s ease;
  display: flex;
  flex-direction: column;

  @media (max-width: 768px) {
    width: 280px;
  }

  :global(.dark) & {
    background: #190233;
    border-right: 2px solid #45058C;
    box-shadow:
      4px 0 32px rgba(0, 0, 0, 0.6),
      2px 0 0 rgba(69, 5, 140, 0.3);
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
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #6508CC 0%, #A30053 25%, #FF084B 50%, #FF5A08 75%, #FFC108 100%);
    border-radius: 0 0 2px 2px;
  }

  :global(.dark) & {
    border-bottom-color: transparent;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);

  :global(.dark) & {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5), 0 0 10px rgba(130, 42, 204, 0.3);
  }
}

.logo-text {
  color: white;
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: -0.02em;

  :global(.dark) & {
    text-shadow: 0 0 14px rgba(192, 132, 252, 0.35);
  }
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
    background: rgba(255, 255, 255, 0.15);
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.25);
  }

  :global(.dark) & {
    &::-webkit-scrollbar-thumb {
      background: rgba(130, 42, 204, 0.35);
    }

    &::-webkit-scrollbar-thumb:hover {
      background: rgba(130, 42, 204, 0.55);
    }
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
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
  border: 1px solid transparent;

  i {
    font-size: 1.2rem;
    flex-shrink: 0;
    width: 24px;
    text-align: center;
    transition: all 0.2s ease;
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
    border-radius: 0 2px 2px 0;
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

  /* Dark mode hover */
  :global(.dark) &:not(.nav-item-active):hover {
    background: rgba(130, 42, 204, 0.12);
    color: rgba(255, 255, 255, 0.95);
    border-color: rgba(130, 42, 204, 0.15);
  }
}

.nav-item-active {
  background: #45058C;
  color: #ffffff;
  font-weight: 600;
  border-color: transparent;
  box-shadow: 0 2px 12px rgba(69, 5, 140, 0.45);

  &::before {
    transform: scaleY(1);
    background: #FF084B;
    box-shadow: 0 0 6px rgba(255, 8, 75, 0.7), 0 0 14px rgba(255, 8, 75, 0.35);
  }

  &:hover {
    background: #5a0aaf;
    color: #ffffff;
    transform: translateX(2px);
  }

  :global(.dark) & {
    background: #45058C;
    color: #ffffff;
    border-color: transparent;
    box-shadow: 0 2px 16px rgba(69, 5, 140, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.08);

    i {
      color: #ffffff;
    }

    &::before {
      background: #FF084B;
      box-shadow: 0 0 8px rgba(255, 8, 75, 0.85), 0 0 20px rgba(255, 8, 75, 0.5);
    }

    &:hover {
      background: #5a0aaf;
      color: #ffffff;
      transform: translateX(2px);
      box-shadow: 0 4px 20px rgba(88, 28, 160, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.1);
    }
  }
}

.admin-item {
  margin-top: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.05);

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.45);
  }

  &.nav-item-active {
    background: rgba(255, 200, 100, 0.92);
    color: var(--primary);
    border-color: rgba(255, 200, 100, 0.9);
    box-shadow: 0 2px 12px rgba(255, 200, 100, 0.3), 0 0 8px rgba(255, 200, 100, 0.2);
  }

  :global(.dark) & {
    border-color: rgba(130, 42, 204, 0.3);
    background: rgba(130, 42, 204, 0.07);

    &:hover {
      background: rgba(130, 42, 204, 0.14);
      border-color: rgba(130, 42, 204, 0.5);
    }

    &.nav-item-active {
      background: rgba(251, 191, 36, 0.14);
      color: #fbbf24;
      border-color: rgba(251, 191, 36, 0.4);
      box-shadow: 0 0 12px rgba(251, 191, 36, 0.2), 0 0 4px rgba(251, 191, 36, 0.3);
    }
  }
}

.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1rem 1.25rem;
  flex-shrink: 0;
  background: rgba(0, 0, 0, 0.08);

  :global(.dark) & {
    border-top-color: rgba(130, 42, 204, 0.2);
    background: rgba(130, 42, 204, 0.04);
  }
}

@media (min-width: 769px) {
  .sidebar {
    position: sticky;
    top: 0;
  }
}

@media (min-width: 769px) and (max-width: 1440px) {
  .sidebar {
    width: 220px;
  }
}
</style>
