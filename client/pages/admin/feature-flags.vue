<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FeatureKey } from '~/constants/featureKeys'
import { FEATURE_KEY_LABELS } from '~/constants/featureKeys'
import adminMiddleware from '~/middleware/admin'
import authMiddleware from '~/middleware/auth'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware, adminMiddleware],
})

const { flags, isFetching, isToggling, fetchFlags, toggleFlag } = useFeatureFlags()
const toastr = useJToast()

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
  <div class="ff-page">
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <i class="pi pi-flag" />
        </div>
        <div>
          <h1 class="page-title">
            Feature Flags
          </h1>
          <p class="page-subtitle">
            Activation et désactivation des fonctionnalités en temps réel
          </p>
        </div>
      </div>
    </div>

    <div class="ff-content">
      <div class="ff-card">
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
            aria-label="Rafraîchir"
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
    </div>
  </div>
</template>

<style scoped lang="scss">
.ff-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--bg-gradient-from) 0%, var(--bg-gradient-to) 100%);
}

.page-header {
  margin-bottom: 2rem;
  max-width: 900px;
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
    flex-shrink: 0;
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

.ff-content {
  max-width: 900px;
  margin: 0 auto;
}

.ff-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 4px 24px var(--shadow-md);
  border: 1px solid var(--card-border);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 32px var(--shadow-lg);
    border-color: var(--primary);
  }
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1.5rem;

  .loading-text {
    color: var(--text-secondary);
    font-size: 1rem;
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
    font-size: 3rem;
    color: var(--text-muted);
    opacity: 0.3;
    margin-bottom: 1rem;
  }

  .empty-text {
    font-size: 1.1rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 0.5rem 0;
  }

  .empty-subtext {
    color: var(--text-secondary);
    margin: 0;
    font-size: 0.9rem;
  }
}

@media (max-width: 768px) {
  .ff-page {
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

  .ff-card {
    padding: 1.25rem;
    border-radius: 16px;
  }

  .card-header {
    margin-bottom: 1.5rem;

    .header-left h2 {
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

  .flag-label {
    white-space: normal;
  }
}
</style>
