<script setup lang="ts">
import { onMounted, ref } from 'vue'
import useAdmin from '~/composables/useAdmin'
import useAuth from '~/composables/useAuth'

definePageMeta({
  layout: 'sidebar-layout',
})

const { isAdmin, register } = useAuth()
const { users, totalUsers, totalPages, currentPage, isLoading, fetchUsers } = useAdmin()
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
      loadUsers()
    },
    (error) => {
      toastr.errorAxios(error)
    },
  )

  isCreating.value = false
}

async function loadUsers(page: number = 0) {
  await fetchUsers(
    page,
    10,
    undefined,
    (error) => {
      toastr.errorAxios(error)
    },
  )
}

function onPageChange(event: any) {
  loadUsers(event.page)
}

function formatDateTime(dateString: string | null) {
  if (!dateString) {
    return 'N/A'
  }

  const date = new Date(dateString)
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function getRoleBadgeClass(roles: string[]) {
  if (roles.includes('ADMIN')) {
    return 'severity-danger'
  }
  return 'severity-info'
}

function getRoleLabel(roles: string[]) {
  if (roles.includes('ADMIN')) {
    return 'Admin'
  }
  return 'Utilisateur'
}

onMounted(() => {
  loadUsers()
})
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
          <div class="header-left">
            <i class="pi pi-user-plus" />
            <h2>Créer un nouveau compte</h2>
          </div>
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

      <div class="users-table-card">
        <div class="card-header">
          <div class="header-left">
            <i class="pi pi-users" />
            <h2>Utilisateurs enregistrés</h2>
          </div>
          <div class="user-count">
            <span class="count-badge">{{ totalUsers }}</span>
            <span class="count-label">utilisateurs</span>
          </div>
        </div>

        <DataTable
          :value="users"
          :loading="isLoading"
          striped-rows
          responsive-layout="scroll"
          class="users-datatable"
          :paginator="false"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-users empty-icon" />
              <p class="empty-text">
                Aucun utilisateur trouvé
              </p>
              <p class="empty-subtext">
                Créez votre premier utilisateur pour commencer
              </p>
            </div>
          </template>

          <template #loading>
            <div class="loading-container">
              <ProgressSpinner
                style="width: 50px; height: 50px"
                stroke-width="4"
              />
              <p class="loading-text">
                Chargement des utilisateurs...
              </p>
            </div>
          </template>

          <Column field="username" header="Nom d'utilisateur" :sortable="true">
            <template #body="{ data }">
              <div class="user-cell">
                <div class="user-avatar-small">
                  <i class="pi pi-user" />
                </div>
                <span class="username-text">{{ data.username }}</span>
              </div>
            </template>
          </Column>

          <Column field="email" header="Email">
            <template #body="{ data }">
              <span v-if="data.email" class="email-text">
                <i class="pi pi-envelope" />
                {{ data.email }}
              </span>
              <span v-else class="text-muted">N/A</span>
            </template>
          </Column>

          <Column field="roles" header="Rôle" :sortable="true">
            <template #body="{ data }">
              <Tag
                :value="getRoleLabel(data.roles)"
                :class="getRoleBadgeClass(data.roles)"
                class="role-badge"
              />
            </template>
          </Column>

          <Column field="createdDate" header="Date de création" :sortable="true">
            <template #body="{ data }">
              <div class="date-cell">
                <i class="pi pi-calendar" />
                <span>{{ formatDateTime(data.createdDate) }}</span>
              </div>
            </template>
          </Column>

          <Column header="Actions" :exportable="false" style="width: 80px">
            <template #body>
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                text
                rounded
                aria-label="Actions"
              />
            </template>
          </Column>
        </DataTable>

        <div v-if="totalPages > 1" class="pagination-container">
          <Paginator
            :first="currentPage * 10"
            :rows="10"
            :total-records="totalUsers"
            @page="onPageChange"
          />
        </div>
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
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;

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
  grid-template-columns: 1fr;
  gap: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;

  @media (min-width: 1200px) {
    grid-template-columns: 400px 1fr;
  }
}

.creation-card,
.users-table-card {
  background: var(--surface-card);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--surface-border);
  transition: all 0.3s ease;
  width: 100%;

  &:hover {
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid var(--surface-border);
    color: var(--text-color);

    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;

      i {
        font-size: 1.5rem;
        color: #667eea;
      }

      h2 {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--text-color);
      }
    }

    .user-count {
      display: flex;
      align-items: center;
      gap: 0.5rem;

      .count-badge {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 12px;
        font-weight: 700;
        font-size: 1.25rem;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      .count-label {
        color: var(--text-color-secondary);
        font-size: 0.95rem;
      }
    }
  }
}

.creation-card {
  height: fit-content;
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

.users-datatable {
  :deep(.p-datatable-wrapper) {
    border-radius: 12px;
    overflow: hidden;
  }

  :deep(.p-datatable-thead > tr > th) {
    color: white;
    font-weight: 600;
    padding: 1rem;
    border: none;
    font-size: 0.95rem;
  }

  :deep(.p-datatable-tbody > tr) {
    transition: all 0.2s ease;
    > td {
      padding: 1rem;
      border-bottom: 1px solid var(--surface-border);
    }
  }

  :deep(.p-datatable-striped .p-datatable-tbody > tr:nth-child(even)) {
    background: var(--surface-50);
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;

  .user-avatar-small {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 0.875rem;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    flex-shrink: 0;
  }

  .username-text {
    font-weight: 600;
    color: var(--text-color);
  }
}

.email-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.9rem;

  i {
    color: #667eea;
    font-size: 0.875rem;
  }
}

.text-muted {
  color: var(--text-color-secondary);
  font-style: italic;
}

.role-badge {
  font-weight: 600;
  font-size: 0.85rem;

  &.severity-danger {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    color: white;
  }

  &.severity-info {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
  }
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.9rem;

  i {
    color: #667eea;
    font-size: 0.875rem;
  }
}

.pagination-container {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;

  :deep(.p-paginator) {
    background: transparent;
    border: none;
    padding: 1rem;
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1.5rem;

  .loading-text {
    color: var(--text-color-secondary);
    font-size: 1.1rem;
    margin: 0;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;

  .empty-icon {
    font-size: 4rem;
    color: var(--text-color-secondary);
    opacity: 0.3;
    margin-bottom: 1rem;
  }

  .empty-text {
    font-size: 1.25rem;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 0.5rem 0;
  }

  .empty-subtext {
    color: var(--text-color-secondary);
    margin: 0;
  }
}

@media (prefers-color-scheme: dark) {
  .creation-card,
  .users-table-card {
    background: var(--surface-card);
    border-color: var(--surface-border);
  }
}

// Responsive
@media (max-width: 1024px) {
  .admin-content {
    max-width: 100%;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-page {
    padding: 1rem;
  }

  .page-header {
    margin-bottom: 1.5rem;

    .header-content {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .header-icon {
      width: 48px;
      height: 48px;
      font-size: 1.5rem;
    }

    .page-title {
      font-size: 1.75rem;
    }

    .page-subtitle {
      font-size: 0.95rem;
    }
  }

  .admin-content {
    gap: 1.5rem;
  }

  .creation-card,
  .users-table-card {
    padding: 1.25rem;
    border-radius: 16px;

    .card-header {
      margin-bottom: 1.5rem;
      padding-bottom: 0.75rem;
      flex-wrap: wrap;

      .header-left {
        gap: 0.75rem;

        i {
          font-size: 1.25rem;
        }

        h2 {
          font-size: 1.25rem;
        }
      }

      .user-count {
        .count-badge {
          padding: 0.375rem 0.75rem;
          font-size: 1rem;
        }

        .count-label {
          font-size: 0.875rem;
        }
      }
    }
  }

  .user-form {
    gap: 1.25rem;

    .form-group {
      gap: 0.375rem;
    }

    .form-label {
      font-size: 0.875rem;
    }

    .form-actions {
      margin-top: 0.5rem;
      gap: 0.75rem;
    }
  }

  .users-datatable {
    font-size: 0.85rem;

    :deep(.p-datatable-thead > tr > th) {
      padding: 0.75rem 0.5rem;
      font-size: 0.85rem;
    }

    :deep(.p-datatable-tbody > tr > td) {
      padding: 0.75rem 0.5rem;
    }
  }

  .user-cell {
    .user-avatar-small {
      width: 32px;
      height: 32px;
      font-size: 0.75rem;
    }

    .username-text {
      font-size: 0.9rem;
    }
  }

  .email-text,
  .date-cell {
    font-size: 0.85rem;

    i {
      font-size: 0.75rem;
    }
  }

  .role-badge {
    font-size: 0.75rem;
    padding: 0.25rem 0.5rem;
  }

  .loading-container {
    padding: 3rem 1.5rem;

    .loading-text {
      font-size: 1rem;
    }
  }

  .empty-state {
    padding: 3rem 1.5rem;

    .empty-icon {
      font-size: 3rem;
    }

    .empty-text {
      font-size: 1.125rem;
    }

    .empty-subtext {
      font-size: 0.9rem;
    }
  }
}

@media (max-width: 480px) {
  .admin-page {
    padding: 0.75rem;
  }

  .page-header {
    margin-bottom: 1.25rem;

    .page-title {
      font-size: 1.5rem;
    }

    .page-subtitle {
      font-size: 0.875rem;
    }
  }

  .admin-content {
    gap: 1.25rem;
  }

  .creation-card,
  .users-table-card {
    padding: 1rem;

    .card-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.75rem;

      .user-count {
        width: 100%;
        justify-content: space-between;
      }
    }
  }

  .form-actions {
    flex-direction: column;

    button {
      width: 100%;
    }
  }

  .users-datatable {
    font-size: 0.8rem;

    :deep(.p-datatable-thead > tr > th) {
      padding: 0.5rem 0.35rem;
      font-size: 0.75rem;
    }

    :deep(.p-datatable-tbody > tr > td) {
      padding: 0.5rem 0.35rem;
    }

    :deep(.p-datatable-wrapper) {
      overflow-x: auto;
    }
  }

  .user-cell {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;

    .user-avatar-small {
      width: 28px;
      height: 28px;
      font-size: 0.7rem;
    }
  }

  .email-text,
  .date-cell {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
    font-size: 0.8rem;

    i {
      display: none;
    }
  }

  .role-badge {
    font-size: 0.7rem;
    padding: 0.2rem 0.4rem;
  }
}
</style>
