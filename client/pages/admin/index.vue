<script setup lang="ts">
import { ref } from 'vue'
import useAuth from '~/composables/useAuth'

definePageMeta({
  layout: 'sidebar-layout',
})

const { isAdmin, register } = useAuth()
const toastr = useJToast()

if (!isAdmin.value) {
  navigateTo('/')
}

const newUser = ref({
  username: '',
  password: '',
  confirmPassword: '',
})

const isCreating = ref(false)

function resetForm() {
  newUser.value = {
    username: '',
    password: '',
    confirmPassword: '',
  }
}

async function createUser() {
  if (!newUser.value.username || !newUser.value.password) {
    toastr.error('Veuillez remplir tous les champs')
    return
  }

  if (newUser.value.password !== newUser.value.confirmPassword) {
    toastr.error('Les mots de passe ne correspondent pas')
    return
  }

  if (newUser.value.password.length < 6) {
    toastr.error('Le mot de passe doit contenir au moins 6 caractères')
    return
  }

  isCreating.value = true

  await register(
    newUser.value,
    () => {
      toastr.success(`Utilisateur ${newUser.value.username} créé avec succès`)
      resetForm()
    },
    (error) => {
      toastr.errorAxios(error)
    },
  )

  isCreating.value = false
}
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <i class="pi pi-shield" />
        </div>
        <div>
          <h1 class="page-title">
            Console d'administration
          </h1>
          <p class="page-subtitle">
            Gestion des comptes utilisateurs
          </p>
        </div>
      </div>
    </div>

    <div class="admin-content">
      <div class="creation-card">
        <div class="card-header">
          <i class="pi pi-user-plus" />
          <h2>Créer un nouveau compte</h2>
        </div>

        <form class="user-form" @submit.prevent="createUser">
          <div class="form-group">
            <label for="username" class="form-label">
              <i class="pi pi-user" />
              Nom d'utilisateur
            </label>
            <InputText
              id="username"
              v-model="newUser.username"
              type="text"
              class="w-full"
              placeholder="Entrez le nom d'utilisateur"
              :disabled="isCreating"
            />
          </div>

          <div class="form-group">
            <label for="password" class="form-label">
              <i class="pi pi-lock" />
              Mot de passe
            </label>
            <InputText
              id="password"
              v-model="newUser.password"
              type="password"
              class="w-full"
              placeholder="Entrez le mot de passe"
              :disabled="isCreating"
            />
            <small class="form-hint">Minimum 6 caractères</small>
          </div>

          <div class="form-group">
            <label for="confirmPassword" class="form-label">
              <i class="pi pi-lock" />
              Confirmer le mot de passe
            </label>
            <InputText
              id="confirmPassword"
              v-model="newUser.confirmPassword"
              type="password"
              class="w-full"
              placeholder="Confirmez le mot de passe"
              :disabled="isCreating"
            />
          </div>

          <div class="form-actions">
            <Button
              type="button"
              label="Réinitialiser"
              icon="pi pi-refresh"
              severity="secondary"
              outlined
              :disabled="isCreating"
              @click="resetForm"
            />
            <Button
              type="submit"
              label="Créer le compte"
              icon="pi pi-check"
              :loading="isCreating"
            />
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.admin-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--surface-ground);
}

.page-header {
  margin-bottom: 2rem;

  .header-content {
    display: flex;
    align-items: center;
    gap: 1.5rem;
  }

  .header-icon {
    width: 64px;
    height: 64px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 2rem;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  }

  .page-title {
    font-size: 2.5rem;
    font-weight: 700;
    margin: 0;
    color: var(--text-color);
  }

  .page-subtitle {
    color: var(--text-color);
    font-size: 1.1rem;
    margin: 0.5rem 0 0 0;
  }
}

.admin-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.creation-card,
.recent-users-card {
  background: var(--surface-card);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--surface-border);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid var(--surface-border);

    i {
      font-size: 1.5rem;
      color: #667eea;
    }

    h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--primary);
    }
  }
}

.user-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .form-label {
    font-weight: 600;
    color: var(--text-color);
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.95rem;

    i {
      color: #667eea;
    }
  }

  .form-hint {
    color: var(--text-color-secondary);
    font-size: 0.875rem;
    margin-top: 0.25rem;
    font-style: italic;
  }

  .form-actions {
    display: flex;
    gap: 1rem;
    margin-top: 1rem;

    button {
      flex: 1;
      font-weight: 600;
    }
  }
}

.recent-users-card {
  .users-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .user-item {
    display: flex;
    padding: 1.25rem;
    background: var(--surface-50);
    background: var(--surface-ground);
    border-radius: 12px;
    border: 1px solid var(--surface-border);
    transition: all 0.3s ease;
    background: var(--surface-100);
    &:hover {
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
      border-color: #667eea;
      transform: translateX(4px);
    }
  }

  .user-avatar {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    font-size: 1.25rem;
    flex-shrink: 0;
  }

  .user-info {
    flex: 1;
  }

  .user-name {
    font-weight: 600;
    color: var(--text-color);
    font-size: 1rem;
    margin-bottom: 0.25rem;
  }

  .user-date {
    color: var(--text-color-secondary);
    font-size: 0.875rem;
  }
    color: #10b981;
  .user-badge {
    color: var(--green-500);
    font-size: 1.5rem;
  }
}

// Animations
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.creation-card,
.recent-users-card {
  animation: slideInUp 0.5s ease;
}

// Mode sombre
@media (prefers-color-scheme: dark) {
  .creation-card,
  .recent-users-card {
    background: var(--surface-card);
    border-color: var(--surface-border);
  }
}

// Responsive
@media (max-width: 768px) {
  .admin-page {
    padding: 1rem;
  }

  .page-header {
    .header-content {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .page-title {
      font-size: 2rem;
    }
  }

  .creation-card,
  .recent-users-card {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column;

    button {
      width: 100%;
    }
  }
}
</style>
