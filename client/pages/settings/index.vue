<script setup lang="ts">
import useChangePassword from '~/composables/useChangePassword'
import useUserSettings from '~/composables/useUserSettings'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import authMiddleware from '~/middleware/auth'

const { emailVerified, userEmail } = useConsent()
const { resendVerificationEmail, isResending, isResendOnCooldown, resendCooldown } = useEmailVerification()

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const { getSettings, updateSettings } = useUserSettings()
const { withLoading, isScopeLoading } = useLoading()
const toast = useJToast()

async function handleResend() {
  await resendVerificationEmail(
    () => toast.success('E-mail de vérification envoyé. Vérifiez votre boîte de réception.'),
    () => toast.error('Impossible d\'envoyer l\'e-mail de vérification.'),
  )
}

const loadSettingsScope = LOADING_SCOPES.settings.load
const saveSettingsScope = LOADING_SCOPES.settings.save

const bookletCycles = ref<BookletMonthlyCycleDTO[]>([])
const projectionWindowDays = ref(15)

const isLoading = computed(() => isScopeLoading(loadSettingsScope))
const isSaving = computed(() => isScopeLoading(saveSettingsScope))

function normalizeProjectionWindowDays(value: number | undefined): number {
  if (value === undefined || Number.isNaN(value)) {
    return 15
  }
  return Math.min(60, Math.max(7, Math.trunc(value)))
}

function normalizeMonthlyPeriodStartDay(value: number): number {
  if (Number.isNaN(value)) {
    return 1
  }
  return Math.min(31, Math.max(1, Math.trunc(value)))
}

function normalizeMonthlyPeriodEndDay(value: number | null | undefined): number | null {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return null
  }

  return Math.min(31, Math.max(1, Math.trunc(value)))
}

function monthlyDayOptions() {
  return Array.from({ length: 31 }, (_, index) => index + 1)
}

async function loadUserSettings() {
  await withLoading(async () => {
    const settings = await getSettings()
    if (!settings) {
      toast.error('Impossible de charger les paramètres utilisateur')
      return
    }

    projectionWindowDays.value = normalizeProjectionWindowDays(settings.projectionWindowDays)
    bookletCycles.value = settings.bookletCycles
      .map(cycle => ({
        bookletId: cycle.bookletId,
        label: cycle.label,
        monthlyPeriodStartDay: normalizeMonthlyPeriodStartDay(cycle.monthlyPeriodStartDay),
        monthlyPeriodEndDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
      }))
      .sort((a, b) => a.label.localeCompare(b.label))
  }, loadSettingsScope)
}

async function saveUserSettings() {
  await withLoading(async () => {
    const payload: UserSettingsUpdateDTO = {
      projectionWindowDays: normalizeProjectionWindowDays(projectionWindowDays.value),
      bookletCycles: bookletCycles.value.map(cycle => ({
        bookletId: cycle.bookletId,
        monthlyPeriodStartDay: normalizeMonthlyPeriodStartDay(cycle.monthlyPeriodStartDay),
        monthlyPeriodEndDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
      })),
    }

    const updatedSettings = await updateSettings(payload)
    if (!updatedSettings) {
      toast.error('Impossible de sauvegarder les paramètres')
      return
    }

    projectionWindowDays.value = normalizeProjectionWindowDays(updatedSettings.projectionWindowDays)
    bookletCycles.value = updatedSettings.bookletCycles
      .map(cycle => ({
        bookletId: cycle.bookletId,
        label: cycle.label,
        monthlyPeriodStartDay: normalizeMonthlyPeriodStartDay(cycle.monthlyPeriodStartDay),
        monthlyPeriodEndDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
      }))
      .sort((a, b) => a.label.localeCompare(b.label))

    toast.success('Paramètres enregistrés')
  }, saveSettingsScope)
}

const {
  currentPassword,
  newPassword: newPasswordChange,
  confirmPassword: confirmPasswordChange,
  confirmPasswordError,
  isSubmitting: isChangingPassword,
  changePassword,
} = useChangePassword()

onMounted(() => {
  loadUserSettings()
})
</script>

<template>
  <div class="settings-page">
    <div class="settings-header">
      <h1>Paramètres utilisateur</h1>
      <p>Configuration globale de la projection et des cycles mensuels par compte.</p>
    </div>

    <div v-if="isLoading" class="settings-loading">
      Chargement des paramètres…
    </div>

    <div v-else class="settings-grid">
      <section class="settings-card">
        <h2>Apparence</h2>
        <p class="settings-help">
          Choisissez le thème visuel de l'application.
        </p>
        <ThemePicker />
      </section>

      <section class="settings-card">
        <h2>Projection</h2>
        <p class="settings-help">
          Définis le nombre de jours à venir, utilisé pour les prévisions sur le dashboard.
        </p>

        <label for="projection-window" class="settings-label">Fenêtre de projection (7 à 60 jours)</label>
        <input
          id="projection-window"
          v-model.number="projectionWindowDays"
          data-test="projection-window-input"
          type="number"
          min="7"
          max="60"
          step="1"
          class="settings-input"
        >
      </section>

      <section class="settings-card">
        <h2>Cycle mensuel par compte</h2>
        <p class="settings-help">
          Configure le jour de début de période pour chaque compte. Le début s'applique au mois précédent du mois affiché. La fin peut être personnalisée ; sans valeur, elle est calculée automatiquement (jour de début du cycle suivant - 1).
        </p>

        <div v-if="bookletCycles.length === 0" class="empty-state">
          Aucun compte disponible pour configurer un cycle mensuel.
        </div>

        <div v-else class="booklet-cycle-list">
          <div v-for="cycle in bookletCycles" :key="cycle.bookletId" class="booklet-cycle-item">
            <div class="booklet-cycle-info">
              <p class="booklet-cycle-label" :title="cycle.label">
                {{ cycle.label }}
              </p>
            </div>

            <div class="booklet-cycle-controls">
              <div class="cycle-field">
                <span class="cycle-field-label">Début</span>
                <select v-model.number="cycle.monthlyPeriodStartDay" class="cycle-select" :data-test="`cycle-select-${cycle.bookletId}`">
                  <option v-for="day in monthlyDayOptions()" :key="day" :value="day">
                    {{ day }}
                  </option>
                </select>
                <span class="cycle-field-hint">Démarre le mois précédent</span>
              </div>

              <div class="cycle-field">
                <span class="cycle-field-label">Fin</span>
                <select v-model="cycle.monthlyPeriodEndDay" class="cycle-select" :data-test="`cycle-end-select-${cycle.bookletId}`">
                  <option :value="null">
                    Par défaut
                  </option>
                  <option v-for="day in monthlyDayOptions()" :key="`end-${day}`" :value="day">
                    {{ day }}
                  </option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="settings-card" data-test="change-password-section">
      <h2>Changer le mot de passe</h2>
      <p class="settings-help">
        Modifiez votre mot de passe de connexion.
      </p>

      <div class="change-password-form">
        <div class="change-password-field">
          <label for="current-password" class="settings-label">Mot de passe actuel</label>
          <input
            id="current-password"
            v-model="currentPassword"
            type="password"
            class="settings-input"
            placeholder="Mot de passe actuel"
            maxlength="100"
            data-test="current-password-input"
          >
        </div>

        <div class="change-password-field">
          <label for="new-password-settings" class="settings-label">Nouveau mot de passe</label>
          <input
            id="new-password-settings"
            v-model="newPasswordChange"
            type="password"
            class="settings-input"
            placeholder="Nouveau mot de passe"
            maxlength="100"
            data-test="new-password-input"
          >
        </div>

        <div class="change-password-field">
          <label for="confirm-password-settings" class="settings-label">Confirmer le mot de passe</label>
          <input
            id="confirm-password-settings"
            v-model="confirmPasswordChange"
            type="password"
            class="settings-input"
            placeholder="Confirmer le mot de passe"
            maxlength="100"
            data-test="confirm-password-input"
          >
          <p v-if="confirmPasswordError" class="change-password-error" data-test="confirm-password-error">
            {{ confirmPasswordError }}
          </p>
        </div>

        <button
          class="save-btn"
          data-test="change-password-btn"
          :disabled="isChangingPassword"
          @click="changePassword"
        >
          {{ isChangingPassword ? 'Modification...' : 'Changer le mot de passe' }}
        </button>
      </div>
    </section>

    <section v-if="!emailVerified" class="settings-card email-verification-card" data-test="email-verification-section">
      <div class="email-verification-header">
        <div class="email-verification-icon">
          <i class="pi pi-exclamation-triangle" aria-hidden="true" />
        </div>
        <div>
          <h2>Vérification de l'e-mail</h2>
          <p class="settings-help">
            Votre adresse e-mail n'est pas encore vérifiée.
          </p>
        </div>
      </div>

      <div v-if="userEmail" class="email-verification-address" data-test="email-display">
        <span class="settings-label">Adresse e-mail</span>
        <span class="email-value">{{ userEmail }}</span>
      </div>

      <button
        class="verify-btn"
        data-test="resend-verification-btn"
        :disabled="isResending || isResendOnCooldown"
        @click="handleResend"
      >
        <i class="pi pi-envelope" aria-hidden="true" />
        <span v-if="isResending">Envoi en cours…</span>
        <span v-else-if="isResendOnCooldown">Renvoyer dans {{ resendCooldown }}s</span>
        <span v-else>Vérifier mon e-mail</span>
      </button>
    </section>

    <div class="actions">
      <button class="save-btn" data-test="save-settings-btn" :disabled="isSaving" @click="saveUserSettings">
        {{ isSaving ? 'Enregistrement...' : 'Enregistrer les paramètres' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 72rem;
  margin: 0 auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.settings-header h1 {
  font-size: 1.9rem;
  margin: 0;
  color: var(--text-primary);
}

.settings-header p {
  margin: 0.4rem 0 0;
  color: var(--text-secondary);
}

.settings-loading {
  border-radius: 0.8rem;
  padding: 1rem;
  background-color: var(--card-bg);
  color: var(--text-secondary);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.settings-card {
  border-radius: 1rem;
  background-color: var(--card-bg);
  border: 1px solid var(--border-color);
  padding: 1rem;
}

.settings-card h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.2rem;
}

.settings-help {
  margin: 0.5rem 0 1rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.settings-label {
  display: block;
  margin-bottom: 0.4rem;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 600;
}

.settings-input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 0.7rem;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  padding: 0.55rem 0.7rem;
}

.booklet-cycle-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.booklet-cycle-item {
  display: grid;
  grid-template-columns: 1fr minmax(240px, 360px);
  gap: 0.75rem;
  align-items: start;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.8rem;
  background-color: var(--bg-tertiary);
}

.booklet-cycle-controls {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
  align-items: start;
}

.cycle-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.cycle-field-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-primary);
}

.cycle-select {
  width: 100%;
  max-width: 110px;
  border: 1.5px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--card-bg);
  color: var(--text-primary);
  padding: 0.45rem 0.4rem;
  cursor: pointer;
  font-size: 0.9rem;
}

.cycle-select:focus {
  border-color: var(--primary);
}

/* Flatten the outline for pointer focus only; the keyboard ring from reset.css must stay. */
.cycle-select:focus:not(:focus-visible) {
  outline: none;
}

.cycle-field-hint {
  font-size: 0.72rem;
  color: var(--text-secondary);
  font-style: italic;
}

.booklet-cycle-label {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.booklet-cycle-info {
  min-width: 0;
  display: flex;
  align-items: center;
}

.empty-state {
  color: var(--text-secondary);
  border: 1px dashed var(--border-color);
  border-radius: 0.7rem;
  padding: 0.8rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  border: none;
  border-radius: 0.75rem;
  padding: 0.65rem 1rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.change-password-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.change-password-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.change-password-error {
  margin: 0;
  font-size: 0.8rem;
  color: #ef4444;
}

.email-verification-card {
  border-color: #f59e0b;
  background-color: #fffbeb;

  .dark & {
    background-color: rgba(245, 158, 11, 0.07);
    border-color: rgba(245, 158, 11, 0.4);
  }
}

.email-verification-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.email-verification-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 50%;
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  font-size: 1rem;

  .dark & {
    background: rgba(245, 158, 11, 0.12);
    color: #fbbf24;
  }
}

.email-verification-address {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 1rem;
}

.email-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}

.verify-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  border-radius: 0.75rem;
  padding: 0.6rem 1.1rem;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

@media (max-width: 640px) {
  .booklet-cycle-item {
    grid-template-columns: 1fr;
  }

  .booklet-cycle-controls {
    grid-template-columns: 1fr;
  }
}
</style>
