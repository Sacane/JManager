<script setup lang="ts">
import useUserSettings from '~/composables/useUserSettings'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

definePageMeta({
  layout: 'sidebar-layout',
})

const { getSettings, updateSettings } = useUserSettings()
const { withLoading, isScopeLoading } = useLoading()
const toast = useJToast()

const loadSettingsScope = LOADING_SCOPES.settings.load
const saveSettingsScope = LOADING_SCOPES.settings.save

const accountCycles = ref<AccountMonthlyCycleDTO[]>([])
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
      toast.error('Impossible de charger les parametres utilisateur')
      return
    }

    projectionWindowDays.value = normalizeProjectionWindowDays(settings.projectionWindowDays)
    accountCycles.value = settings.accountCycles
      .map(cycle => ({
        accountId: cycle.accountId,
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
      accountCycles: accountCycles.value.map(cycle => ({
        accountId: cycle.accountId,
        monthlyPeriodStartDay: normalizeMonthlyPeriodStartDay(cycle.monthlyPeriodStartDay),
        monthlyPeriodEndDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
      })),
    }

    const updatedSettings = await updateSettings(payload)
    if (!updatedSettings) {
      toast.error('Impossible de sauvegarder les parametres')
      return
    }

    projectionWindowDays.value = normalizeProjectionWindowDays(updatedSettings.projectionWindowDays)
    accountCycles.value = updatedSettings.accountCycles
      .map(cycle => ({
        accountId: cycle.accountId,
        label: cycle.label,
        monthlyPeriodStartDay: normalizeMonthlyPeriodStartDay(cycle.monthlyPeriodStartDay),
        monthlyPeriodEndDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
      }))
      .sort((a, b) => a.label.localeCompare(b.label))

    toast.success('Parametres enregistres')
  }, saveSettingsScope)
}

onMounted(() => {
  loadUserSettings()
})
</script>

<template>
  <div class="settings-page">
    <div class="settings-header">
      <h1>Parametres utilisateur</h1>
      <p>Configuration globale de la projection et des cycles mensuels par compte.</p>
    </div>

    <div v-if="isLoading" class="settings-loading">
      Chargement des parametres...
    </div>

    <div v-else class="settings-grid">
      <section class="settings-card">
        <h2>Projection</h2>
        <p class="settings-help">
          Definis le nombre de jours à venir, utilisé pour les previsions sur le dashboard.
        </p>

        <label for="projection-window" class="settings-label">Fenetre de projection (7 a 60 jours)</label>
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
          Configure le jour de debut de periode et, si besoin, le jour de fin du mois suivant. Sans jour de fin, le comportement par defaut reste jour du mois suivant - 1.
        </p>

        <div v-if="accountCycles.length === 0" class="empty-state">
          Aucun compte disponible pour configurer un cycle mensuel.
        </div>

        <div v-else class="account-cycle-list">
          <div v-for="cycle in accountCycles" :key="cycle.accountId" class="account-cycle-item">
            <div class="account-cycle-info">
              <p class="account-cycle-label">
                {{ cycle.label }}
              </p>
              <p class="account-cycle-id">
                {{ cycle.accountId }}
              </p>
            </div>

            <div class="account-cycle-controls">
              <label class="account-cycle-control">
                <span class="settings-label">Debut</span>
                <select v-model.number="cycle.monthlyPeriodStartDay" class="settings-select" :data-test="`cycle-select-${cycle.accountId}`">
                  <option v-for="day in monthlyDayOptions()" :key="day" :value="day">
                    Jour {{ day }}
                  </option>
                </select>
              </label>

              <label class="account-cycle-control">
                <span class="settings-label">Fin (mois suivant)</span>
                <select v-model="cycle.monthlyPeriodEndDay" class="settings-select" :data-test="`cycle-end-select-${cycle.accountId}`">
                  <option :value="null">
                    Defaut (jour suivant - 1)
                  </option>
                  <option v-for="day in monthlyDayOptions()" :key="`end-${day}`" :value="day">
                    Jour {{ day }}
                  </option>
                </select>
              </label>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="actions">
      <button class="save-btn" data-test="save-settings-btn" :disabled="isSaving" @click="saveUserSettings">
        {{ isSaving ? 'Enregistrement...' : 'Enregistrer les parametres' }}
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

.settings-input,
.settings-select {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 0.7rem;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  padding: 0.55rem 0.7rem;
}

.account-cycle-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.account-cycle-item {
  display: grid;
  grid-template-columns: 1fr minmax(240px, 360px);
  gap: 0.75rem;
  align-items: center;
  padding: 0.7rem;
  border: 1px solid var(--border-color);
  border-radius: 0.8rem;
  background-color: var(--bg-tertiary);
}

.account-cycle-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 0.6rem;
}

.account-cycle-control {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.account-cycle-label {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
}

.account-cycle-id {
  margin: 0.2rem 0 0;
  color: var(--text-tertiary);
  font-size: 0.75rem;
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
  background: linear-gradient(135deg, #2167f3 0%, #1c4db6 100%);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .account-cycle-item {
    grid-template-columns: 1fr;
  }

  .account-cycle-controls {
    grid-template-columns: 1fr;
  }
}
</style>
