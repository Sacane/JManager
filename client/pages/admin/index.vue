<script setup lang="ts">
import type { AppTableColumn } from '~/components/AppTable.vue'
import type { FeatureKey } from '~/constants/featureKeys'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import useAdmin from '~/composables/useAdmin'
import { FEATURE_KEY_LABELS } from '~/constants/featureKeys'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import adminMiddleware from '~/middleware/admin'
import authMiddleware from '~/middleware/auth'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware, adminMiddleware],
})

// ── User management ──────────────────────────────────────────────────────────

const { users, totalUsers, totalPages, currentPage, isLoading, fetchUsers, createUser } = useAdmin()
const { isScopeLoading } = useLoading()
const toastr = useJToast()

const newUser = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const userCreationLoadingScope = LOADING_SCOPES.admin.createUser
const isCreating = computed(() => isScopeLoading(userCreationLoadingScope))
const isMobileWidth = ref(false)

function resetForm() {
  newUser.value = {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  }
}

async function submitCreateUser() {
  if (!newUser.value.username || !newUser.value.email || !newUser.value.password) {
    toastr.error('Veuillez remplir tous les champs')
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/
  if (!emailRegex.test(newUser.value.email)) {
    toastr.error('L\'adresse email n\'est pas valide')
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

  await createUser(
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

function updateIsMobile() {
  if (typeof window !== 'undefined') {
    isMobileWidth.value = window.innerWidth <= 1024
  }
}

const adminUserColumns: AppTableColumn[] = [
  { field: 'username', header: 'Nom d\'utilisateur', sortable: true, slotName: 'username' },
  { field: 'email', header: 'Email', slotName: 'email' },
  { field: 'roles', header: 'Rôle', sortable: true, slotName: 'roles' },
  { field: 'createdDate', header: 'Date de création', sortable: true, slotName: 'createdDate' },
  { header: 'Actions', exportable: false, style: 'width: 80px', slotName: 'actions' },
]

onMounted(() => {
  loadUsers()
  updateIsMobile()
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', updateIsMobile)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateIsMobile)
  }
})

// ── Feature Flags ─────────────────────────────────────────────────────────────
// Write access (toggleFlag) is intentionally restricted to this admin console.
// The composable itself is stateless regarding auth — access control is enforced
// by the adminMiddleware declared on this page.

const { flags, isFetching, isToggling, fetchFlags, toggleFlag } = useFeatureFlags()
const togglingKey = ref<FeatureKey | null>(null)

const flagsWithMeta = computed(() =>
  flags.value.map(flag => ({
    ...flag,
    label: FEATURE_KEY_LABELS[flag.key as FeatureKey] ?? flag.key,
  })),
)

async function onToggle(key: FeatureKey, enabled: boolean) {
  togglingKey.value = key
  try {
    await toggleFlag(key, enabled)
    toastr.success(
      enabled
        ? `Flag "${FEATURE_KEY_LABELS[key] ?? key}" activé`
        : `Flag "${FEATURE_KEY_LABELS[key] ?? key}" désactivé`,
    )
  } catch {
    toastr.error('Impossible de modifier le flag. Veuillez réessayer.')
    await fetchFlags()
  } finally {
    togglingKey.value = null
  }
}

function isKeyToggling(key: FeatureKey): boolean {
  return isToggling.value && togglingKey.value === key
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
            Gestion des comptes utilisateurs et des fonctionnalités
          </p>
        </div>
      </div>
    </div>

    <div class="admin-tabs-wrapper">
      <Tabs value="users">
        <TabList>
          <Tab value="users">
            <i class="pi pi-users tab-icon" />
            <span>Utilisateurs</span>
          </Tab>
          <Tab value="feature-flags">
            <i class="pi pi-flag tab-icon" />
            <span>Feature Flags</span>
          </Tab>
        </TabList>

        <TabPanels>
          <!-- ── Tab 1 : User management ─────────────────────────────────── -->
          <TabPanel value="users">
            <div class="admin-users-content">
              <div class="creation-card">
                <div class="card-header">
                  <div class="header-left">
                    <i class="pi pi-user-plus" />
                    <h2>Créer un nouveau compte</h2>
                  </div>
                </div>

                <form class="user-form" @submit.prevent="submitCreateUser">
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
                    <label for="email" class="form-label">
                      <i class="pi pi-envelope" />
                      Adresse email
                    </label>
                    <InputText
                      id="email"
                      v-model="newUser.email"
                      type="email"
                      class="w-full"
                      placeholder="Entrez l'adresse email"
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
                  <div v-if="!isMobileWidth" class="user-count">
                    <span class="count-badge">{{ totalUsers }}</span>
                    <span class="count-label">utilisateurs</span>
                  </div>
                </div>

                <!-- Mobile list view: shown only on small screens -->
                <div v-if="isMobileWidth" class="users-mobile-list">
                  <div v-if="isLoading" class="loading-container">
                    <ProgressSpinner
                      style="width: 50px; height: 50px"
                      stroke-width="4"
                    />
                    <p class="loading-text">
                      Chargement des utilisateurs...
                    </p>
                  </div>
                  <template v-else>
                    <div class="users-mobile-count">
                      {{ users.length }} utilisateur(s)
                    </div>
                    <div v-for="user in users" :key="user.id" class="user-mobile-card">
                      <div class="user-mobile-row">
                        <div class="user-avatar-small">
                          <i class="pi pi-user" />
                        </div>

                        <div class="user-mobile-info">
                          <div class="user-mobile-top">
                            <div class="user-mobile-title">
                              <span class="username-text">{{ user.username }}</span>
                              <Tag
                                :value="getRoleLabel(user.roles)"
                                :class="getRoleBadgeClass(user.roles)"
                                class="role-badge-mobile"
                              />
                            </div>
                          </div>

                          <div class="user-mobile-meta">
                            <div v-if="user.email" class="email-text">
                              <i class="pi pi-envelope" />
                              <span class="email-val">{{ user.email }}</span>
                            </div>
                            <div v-else class="text-muted">
                              N/A
                            </div>

                            <div class="created-date-inline">
                              {{ formatDateTime(user.createdDate) }}
                            </div>
                          </div>
                        </div>

                        <div class="user-mobile-actions">
                          <Button
                            class="user-action-btn"
                            icon="pi pi-ellipsis-v"
                            severity="secondary"
                            text
                            rounded
                            aria-label="Actions"
                          />
                        </div>
                      </div>
                    </div>
                  </template>
                </div>

                <AppTable
                  v-else
                  :columns="adminUserColumns"
                  :rows="users"
                  data-key="id"
                  :loading="isLoading"
                  class="users-datatable"
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

                  <template #body-username="{ data }">
                    <div class="user-cell">
                      <div class="user-avatar-small">
                        <i class="pi pi-user" />
                      </div>
                      <span class="username-text">{{ data.username }}</span>
                    </div>
                  </template>

                  <template #body-email="{ data }">
                    <span v-if="data.email" class="email-text">
                      <i class="pi pi-envelope" />
                      {{ data.email }}
                    </span>
                    <span v-else class="text-muted">N/A</span>
                  </template>

                  <template #body-roles="{ data }">
                    <Tag
                      :value="getRoleLabel(data.roles)"
                      :class="getRoleBadgeClass(data.roles)"
                      class="role-badge"
                    />
                  </template>

                  <template #body-createdDate="{ data }">
                    <div class="date-cell">
                      <i class="pi pi-calendar" />
                      <span>{{ formatDateTime(data.createdDate) }}</span>
                    </div>
                  </template>

                  <template #body-actions>
                    <Button
                      icon="pi pi-ellipsis-v"
                      severity="secondary"
                      text
                      rounded
                      aria-label="Actions"
                    />
                  </template>
                </AppTable>

                <div v-if="totalPages > 1" class="pagination-container">
                  <Paginator
                    :first="currentPage * 10"
                    :rows="10"
                    :total-records="totalUsers"
                    :disabled="isLoading"
                    @page="onPageChange"
                  />
                </div>
              </div>
            </div>
          </TabPanel>

          <!-- ── Tab 2 : Feature Flags (admin write only) ───────────────── -->
          <TabPanel value="feature-flags">
            <div class="flags-card">
              <div class="card-header">
                <div class="header-left">
                  <i class="pi pi-sliders-h" />
                  <h2>Fonctionnalités disponibles</h2>
                </div>
                <Button
                  icon="pi pi-refresh"
                  severity="secondary"
                  text
                  rounded
                  :loading="isFetching"
                  aria-label="Rafraîchir les flags"
                  @click="fetchFlags()"
                />
              </div>

              <div v-if="isFetching && flagsWithMeta.length === 0" class="loading-container">
                <ProgressSpinner style="width: 48px; height: 48px" stroke-width="4" />
                <p class="loading-text">
                  Chargement des flags...
                </p>
              </div>

              <div v-else-if="flagsWithMeta.length === 0" class="empty-state">
                <i class="pi pi-inbox empty-icon" />
                <p class="empty-text">
                  Aucun feature flag configuré
                </p>
                <p class="empty-subtext">
                  Ajoutez des clés dans l'enum FeatureKey du backend pour les voir apparaître ici.
                </p>
              </div>

              <ul v-else class="flag-list">
                <li
                  v-for="flag in flagsWithMeta"
                  :key="flag.key"
                  class="flag-row"
                >
                  <div class="flag-info">
                    <span class="flag-label">{{ flag.label }}</span>
                    <code class="flag-key">{{ flag.key }}</code>
                  </div>

                  <div class="flag-control">
                    <Tag
                      :value="flag.enabled ? 'Actif' : 'Inactif'"
                      :class="flag.enabled ? 'tag-enabled' : 'tag-disabled'"
                      class="flag-status-tag"
                    />
                    <ToggleSwitch
                      :model-value="flag.enabled"
                      :disabled="isKeyToggling(flag.key as FeatureKey)"
                      :aria-label="`Activer ou désactiver ${flag.label}`"
                      @update:model-value="(val: boolean) => onToggle(flag.key as FeatureKey, val)"
                    />
                  </div>
                </li>
              </ul>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  </div>
</template>

<style scoped lang="scss">
.admin-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--bg-gradient-from) 0%, var(--bg-gradient-to) 100%);
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
    background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 2rem;
    box-shadow: 0 8px 24px var(--shadow-purple);
  }

  .page-title {
    font-size: 2.5rem;
    font-weight: 700;
    margin: 0;
    color: var(--text-primary);
  }

  .page-subtitle {
    color: var(--text-secondary);
    font-size: 1.1rem;
    margin: 0.5rem 0 0 0;
  }
}

// ── Tabs shell ───────────────────────────────────────────────────────────────

.admin-tabs-wrapper {
  max-width: 1400px;
  margin: 0 auto;

  .tab-icon {
    margin-right: 0.4rem;
    font-size: 1rem;
  }

  :deep(.p-tabs-tablist) {
    background: transparent;
    border-color: var(--border-color);
    margin-bottom: 1.5rem;
  }

  :deep(.p-tabs-tab) {
    font-weight: 500;
    gap: 0.5rem;
  }

  :deep(.p-tabpanels) {
    background: transparent;
    padding: 0;
  }

  :deep(.p-tabpanel) {
    padding: 0;
  }
}

// ── Users tab ────────────────────────────────────────────────────────────────

.admin-users-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
  width: 100%;

  @media (min-width: 1200px) {
    grid-template-columns: 400px 1fr;
  }
}

.creation-card,
.users-table-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 4px 24px var(--shadow-md);
  border: 1px solid var(--card-border);
  transition: all 0.3s ease;
  width: 100%;

  &:hover {
    box-shadow: 0 8px 32px var(--shadow-lg);
    border-color: var(--primary);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid var(--border-color);

    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;

      i {
        font-size: 1.5rem;
        color: var(--primary);
      }

      h2 {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--text-primary);
      }
    }

    .user-count {
      display: flex;
      align-items: center;
      gap: 0.5rem;

      .count-badge {
        background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 12px;
        font-weight: 700;
        font-size: 1.25rem;
        box-shadow: 0 4px 12px var(--shadow-purple);
      }

      .count-label {
        color: var(--text-secondary);
        font-size: 0.95rem;
      }
    }
  }
}

.creation-card {
  height: fit-content;
  background: linear-gradient(135deg, var(--card-bg) 0%, var(--bg-tertiary) 100%);
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
    color: var(--text-primary);
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.95rem;

    i {
      color: var(--primary);
    }
  }

  .form-hint {
    color: var(--text-tertiary);
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

.users-table-card {
  background: var(--card-bg);
}

.users-datatable {
  width: 100%;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;

  .user-avatar-small {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 0.875rem;
    box-shadow: 0 2px 8px var(--shadow-purple);
    flex-shrink: 0;
  }

  .username-text {
    font-weight: 600;
    color: var(--text-primary);
  }
}

.email-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.9rem;

  i {
    color: var(--primary);
    font-size: 0.875rem;
  }
}

.text-muted {
  color: var(--text-tertiary);
  font-style: italic;
}

.role-badge {
  font-weight: 600;
  font-size: 0.85rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;

  &.severity-danger {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
  }

  &.severity-info {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  }
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.9rem;

  i {
    color: var(--primary);
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

    .p-paginator-page,
    .p-paginator-next,
    .p-paginator-prev,
    .p-paginator-first,
    .p-paginator-last {
      color: var(--text-primary);
      background: var(--bg-tertiary);
      border: 1px solid var(--border-color);

      &:hover {
        background: var(--card-hover-bg);
        border-color: var(--primary);
      }

      &.p-highlight {
        background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
        color: white;
        border-color: var(--primary);
      }
    }
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
    color: var(--text-secondary);
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
    color: var(--text-muted);
    opacity: 0.3;
    margin-bottom: 1rem;
  }

  .empty-text {
    font-size: 1.25rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 0.5rem 0;
  }

  .empty-subtext {
    color: var(--text-secondary);
    margin: 0;
  }
}

.users-mobile-list {
  display: none;
}

// ── Feature Flags tab ─────────────────────────────────────────────────────────

.flags-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 4px 24px var(--shadow-md);
  border: 1px solid var(--card-border);
  max-width: 900px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 32px var(--shadow-lg);
    border-color: var(--primary);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid var(--border-color);

    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;

      i {
        font-size: 1.5rem;
        color: var(--primary);
      }

      h2 {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--text-primary);
      }
    }
  }
}

.flag-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.flag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-radius: 12px;
  background: var(--bg-tertiary);
  border: 1px solid var(--card-border);
  transition: background 0.2s ease, border-color 0.2s ease;

  &:hover {
    border-color: var(--primary);
    background: var(--card-hover-bg, var(--bg-tertiary));
  }
}

.flag-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.flag-label {
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--text-primary);
}

.flag-key {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.78rem;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flag-control {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-shrink: 0;
}

.flag-status-tag {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.3rem 0.75rem;
  border-radius: 8px;
  white-space: nowrap;

  &.tag-enabled {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
  }

  &.tag-disabled {
    background: var(--bg-secondary);
    color: var(--text-tertiary);
    border: 1px solid var(--border-color);
  }
}

// ── Responsive ────────────────────────────────────────────────────────────────

@media (max-width: 1024px) {
  .admin-users-content {
    max-width: 100%;
    grid-template-columns: 1fr;
  }

  .users-datatable {
    display: none !important;
  }

  .users-mobile-list {
    display: block !important;
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
        width: 100%;
        justify-content: flex-start;

        .count-badge {
          padding: 0.4rem 0.75rem;
          font-size: 1rem;
        }

        .count-label {
          font-size: 0.85rem;
        }
      }
    }
  }

  .user-form {
    .form-actions {
      flex-direction: column;

      button {
        width: 100%;
      }
    }

    .form-label {
      font-size: 0.92rem;

      i {
        font-size: 0.95rem;
      }
    }
  }

  /* Mobile users list: hide DataTable, show stacked cards */
  .users-datatable {
    display: none !important;
  }

  /* Force show mobile list on small screens */
  .users-mobile-list {
    display: block !important;
    width: 100%;
  }

  .user-mobile-card {
    background: linear-gradient(135deg, var(--bg-tertiary) 0%, var(--card-bg) 100%);
    border: 1px solid var(--card-border);
    border-radius: 12px;
    padding: 0.9rem;
    margin-bottom: 0.9rem;
    box-shadow: 0 2px 10px var(--shadow-sm);
    color: var(--text-primary);
  }

  .user-mobile-row {
    display: grid;
    grid-template-columns: 48px 1fr auto;
    gap: 0.75rem;
    align-items: center;
  }

  .user-mobile-info {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    min-width: 0;
  }

  .user-mobile-title {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    justify-content: space-between;
    min-width: 0;
  }

  .user-mobile-top {
    display: block;
  }

  .user-mobile-meta {
    color: var(--text-secondary);
    font-size: 0.92rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    align-items: flex-start;
  }

  .created-date-inline {
    font-size: 0.82rem;
    color: var(--text-tertiary);
  }

  .user-mobile-card .user-avatar-small {
    min-width: 48px;
    min-height: 48px;
    width: 48px;
    height: 48px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 1.05rem;
  }

  .user-action-btn {
    min-width: 40px;
    height: 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0.25rem;
  }

  .users-mobile-count {
    font-size: 0.95rem;
    color: var(--text-secondary);
    margin-bottom: 0.5rem;
  }

  .flags-card {
    padding: 1.25rem;
    border-radius: 16px;

    .card-header h2 {
      font-size: 1.2rem;
    }
  }

  .flag-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 1rem;
  }

  .flag-control {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
