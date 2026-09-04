<script setup lang="ts">
import type { AxiosError } from 'axios'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import authMiddleware from '~/middleware/auth'
import useBooklet from '../../composables/useBooklet'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const confirm = useConfirm()
const router = useRouter()
const jToast = useJToast()
const { isScopeLoading, withLoading } = useLoading()

const { fetch, deleteBooklet, createBooklet } = useBooklet()
const loadBookletsScope = LOADING_SCOPES.bookletIndex.load
const createBookletScope = LOADING_SCOPES.bookletIndex.create
const deleteBookletScope = LOADING_SCOPES.bookletIndex.delete
const isLoadingBooklets = computed(() => isScopeLoading(loadBookletsScope))
const isCreatingBooklet = computed(() => isScopeLoading(createBookletScope))
const isDeletingBooklet = computed(() => isScopeLoading(deleteBookletScope))
const isAnyBookletActionLoading = computed(() =>
  isLoadingBooklets.value
  || isCreatingBooklet.value
  || isDeletingBooklet.value,
)
const isBookletsFilled = ref<boolean>(false)
const data = ref<Array<{
  id: string
  label: string
  amount: string
  currency: string
}>>([])

const { orderedItems, draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop, onDragEnd } = useBookletOrder(data)

onMounted(async () => {
  await loadBooklets()
})

async function loadBooklets() {
  await withLoading(async () => {
    try {
      const bookletArray = await fetch()
      format(bookletArray)
      isBookletsFilled.value = bookletArray.length > 0
    } catch (err) {
      jToast.errorAxios(err as AxiosError, 'Impossible de charger les livrets')
    }
  }, loadBookletsScope)
}

function format(booklets: Array<BookletDTO>) {
  data.value = booklets.map((booklet: BookletDTO) => ({
    id: String(booklet.id || ''),
    label: booklet.label,
    amount: `${booklet.amount}`,
    currency: booklet.currency || '€',
  }))
}

function onCardClick(bookletId: string) {
  router.push(`/booklet/${bookletId}`)
}

async function applyDelete(bookletId: string) {
  await withLoading(async () => {
    try {
      await deleteBooklet(bookletId)
      jToast.success('Le livret a bien été supprimé')
      await loadBooklets()
    } catch (err) {
      jToast.errorAxios(err as AxiosError, 'Impossible de supprimer le livret')
    }
  }, deleteBookletScope)
}

function openConfirmDeleteDialog(id: string, bookletLabel: string) {
  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer le livret "${bookletLabel}" ? \n Cette action est irréversible et supprimera définitivement toutes les transactions enregistrées dessus.`,
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Supprimer',
    rejectLabel: 'Annuler',
    acceptClass: 'p-button-danger',
    accept: () => applyDelete(id),
  })
}

const isAddBookletDialogOpen = ref<boolean>(false)

function handleBookletCreation(booklet: { label: string, digit: number }) {
  withLoading(async () => {
    try {
      await createBooklet(booklet.label, booklet.digit, '€')
      jToast.success('Le livret a bien été créé')
      await loadBooklets()
    } catch (err) {
      jToast.errorAxios(err as AxiosError, 'Impossible de créer le livret')
    } finally {
      isAddBookletDialogOpen.value = false
    }
  }, createBookletScope)
}

function cancel() {
  isAddBookletDialogOpen.value = false
}

function openBookletDialog() {
  if (isAnyBookletActionLoading.value) return
  isAddBookletDialogOpen.value = true
}

function amountClass(amount: string) {
  return Number.parseFloat(amount) >= 0 ? 'positive' : 'negative'
}

function formatAmount(amount: string) {
  const num = Number.parseFloat(amount)
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num)
}
</script>

<template>
  <ConfirmDialog />
  <div class="page-shell max-w-[1600px]">
    <!-- Header Section -->
    <div class="page-header pb-8 border-b-2 border-[var(--border-color)]">
      <div class="header-left">
        <div class="header-icon">
          <i class="pi pi-wallet" />
        </div>
        <div class="header-text">
          <h1 class="page-heading">
            Mes Livrets
          </h1>
          <p class="page-subheading flex items-center gap-2">
            <span class="count-badge">{{ data.length }}/6</span>
            livrets actifs
          </p>
        </div>
      </div>
      <Button
        v-if="data.length < 6"
        label="Nouveau livret"
        icon="pi pi-plus"
        class="add-button"
        :loading="isCreatingBooklet"
        :disabled="isAnyBookletActionLoading"
        @click="openBookletDialog"
      />
      <Button
        v-else
        label="Limite atteinte"
        icon="pi pi-lock"
        class="add-button disabled-button"
        disabled
      />
    </div>

    <div v-if="isLoadingBooklets" class="loading-container">
      <ProgressSpinner
        style="width: 48px; height: 48px"
        stroke-width="4"
      />
      <p class="loading-text">
        Chargement des livrets...
      </p>
    </div>

    <!-- Empty State -->
    <div v-else-if="!isBookletsFilled" class="empty-state">
      <div class="empty-illustration">
        <i class="pi pi-wallet" />
      </div>
      <h3 class="empty-title">
        Commencez votre parcours d'épargne
      </h3>
      <p class="empty-description">
        Créez votre premier livret et suivez l'évolution de votre épargne en temps réel
      </p>
      <Button
        label="Créer mon premier livret"
        icon="pi pi-plus"
        size="large"
        class="empty-action-button"
        :loading="isCreatingBooklet"
        :disabled="isAnyBookletActionLoading"
        @click="openBookletDialog"
      />
    </div>

    <!-- Booklets Grid -->
    <div v-else class="booklets-container">
      <div class="booklets-grid">
        <!-- Existing Booklets -->
        <div
          v-for="(booklet, index) in orderedItems"
          :key="booklet.id"
          class="booklet-card"
          :class="{ 'is-dragging': draggedIndex === index, 'drag-over': dragOverIndex === index && draggedIndex !== index }"
          draggable="true"
          @click="onCardClick(booklet.id)"
          @dragstart="onDragStart($event, index)"
          @dragover="onDragOver($event, index)"
          @drop="onDrop($event, index)"
          @dragend="onDragEnd"
        >
          <div class="card-inner">
            <!-- Card Header -->
            <div class="card-top">
              <div class="card-top-left">
                <div class="card-icon-wrapper">
                  <i class="pi pi-wallet" />
                </div>
                <h3 class="booklet-name">
                  {{ capitalizeFirst(booklet.label) }}
                </h3>
              </div>
              <div class="card-actions">
                <button
                  v-tooltip.top="'Réorganiser'"
                  class="drag-handle"
                  @click.stop
                >
                  <i class="pi pi-bars" />
                </button>
                <Button
                  icon="pi pi-trash"
                  class="delete-btn"
                  text
                  rounded
                  severity="danger"
                  :loading="isDeletingBooklet"
                  :disabled="isAnyBookletActionLoading"
                  @click.stop="openConfirmDeleteDialog(booklet.id, booklet.label)"
                />
              </div>
            </div>

            <!-- Card Body -->
            <div class="card-middle">
              <div class="booklet-amount">
                <span class="amount-value" :class="amountClass(booklet.amount)">
                  {{ formatAmount(booklet.amount) }}
                </span>
                <span class="amount-currency">{{ booklet.currency }}</span>
              </div>
            </div>

            <!-- Card Footer -->
            <div class="card-bottom">
              <span class="action-link">
                Voir les détails
                <i class="pi pi-arrow-right" />
              </span>
            </div>
          </div>
        </div>

        <!-- Add New Card (if under limit) -->
        <div
          v-if="data.length < 6"
          class="booklet-card add-card"
          :class="{ 'disabled-card': isAnyBookletActionLoading }"
          @click="openBookletDialog"
        >
          <div class="add-card-content">
            <div class="add-icon">
              <i class="pi pi-plus" />
            </div>
            <span class="add-text">Ajouter un livret</span>
            <span class="add-subtext">{{ 6 - data.length }} emplacement{{ 6 - data.length > 1 ? 's' : '' }} restant{{ 6 - data.length > 1 ? 's' : '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <BookletBookingDialog
      :visible="isAddBookletDialogOpen"
      @create-booklet="handleBookletCreation"
      @cancel="cancel"
    />
  </div>
</template>

<style scoped lang="scss">
/* ===== HEADER ===== */
.header-left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.header-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.75rem;
  box-shadow: 0 8px 24px var(--shadow-purple);
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.75rem;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  color: white;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  box-shadow: 0 2px 8px var(--shadow-purple);
}

.add-button {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  border: none;
  padding: 0.875rem 1.75rem;
  font-weight: 600;
  box-shadow: 0 4px 16px var(--shadow-purple);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 1rem;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px var(--shadow-purple);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  @media (max-width: 768px) {
    width: 100%;
  }
}

.disabled-button {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--text-muted) !important;
}

/* ===== EMPTY STATE ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rem 2rem;
  background: var(--card-bg);
  border-radius: 24px;
  box-shadow: 0 4px 24px var(--shadow-md);
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
  border: 1px solid var(--card-border);
}

.empty-illustration {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
  box-shadow: 0 12px 40px var(--shadow-purple);

  i {
    font-size: 3.5rem;
    color: white;
  }
}

.empty-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 1rem 0;
}

.empty-description {
  font-size: 1.05rem;
  color: var(--text-secondary);
  margin: 0 0 2.5rem 0;
  max-width: 400px;
  line-height: 1.6;
}

.empty-action-button {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  border: none;
  padding: 1rem 2.5rem;
  font-weight: 600;
  font-size: 1.05rem;
  box-shadow: 0 4px 16px var(--shadow-purple);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px var(--shadow-purple);
  }
}

/* ===== BOOKLETS GRID ===== */
.booklets-container {
  width: 100%;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1.5rem 1rem;
  margin-bottom: 1rem;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 16px;
}

.loading-text {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 500;
}

.booklets-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
}

.disabled-card {
  opacity: 0.6;
  pointer-events: none;
}

/* ===== BOOKLET CARD ===== */
.booklet-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 20px;
  border: 2px solid var(--card-border);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &.is-dragging {
    opacity: 0.4;
    transform: scale(0.97) !important;
    box-shadow: none !important;
    border-color: var(--border-color) !important;
  }

  &.drag-over {
    border-color: var(--primary) !important;
    box-shadow: 0 0 0 3px rgba(101, 8, 204, 0.2), 0 12px 40px var(--shadow-lg) !important;
    transform: translateY(-4px) !important;
  }

  &:hover {
    transform: translateY(-4px);
    border-color: var(--primary);
    box-shadow: 0 12px 40px var(--shadow-lg);

    .action-link {
      opacity: 1;
      transform: translateX(0);
    }

    .card-icon-wrapper {
      transform: scale(1.05);
    }
  }

  &:active {
    transform: translateY(-2px);
  }
}

.card-inner {
  padding: 2rem;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 280px;
}

/* Card Top */
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  gap: 1rem;
}

.card-top-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 0;
}

.card-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
  box-shadow: 0 8px 20px var(--shadow-purple);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: grab;
  border-radius: 8px;
  transition: all 0.2s ease;
  padding: 0;

  &:active {
    cursor: grabbing;
  }

  &:hover {
    color: var(--primary);
    background: rgba(101, 8, 204, 0.1);
  }

  i {
    font-size: 0.9rem;
  }
}

.delete-btn {
  color: var(--text-tertiary) !important;
  transition: all 0.2s ease;
  flex-shrink: 0;

  &:hover {
    color: var(--danger) !important;
    background: rgba(239, 68, 68, 0.1) !important;
  }
}

/* Card Middle */
.card-middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.booklet-name {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0;
  word-break: break-word;
  line-height: 1.2;
  flex: 1;
  min-width: 0;

  @media (max-width: 768px) {
    font-size: 1.25rem;
  }
}

.booklet-amount {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.amount-value {
  font-size: 2.25rem;
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;

  /* Income/expense tokens are not used here: a balance sign reads as healthy or not,
     not as money coming in or going out — see var(--income) / var(--expense) usage in
     pages/booklet/[id].vue for the amount case. */
  &.positive {
    color: var(--success);
  }

  &.negative {
    color: var(--danger);
  }
}

.amount-currency {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-tertiary);
}

/* Card Bottom */
.card-bottom {
  margin-top: auto;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--primary);
  opacity: 0.7;
  transform: translateX(-4px);
  transition: all 0.3s ease;

  i {
    font-size: 0.75rem;
    transition: transform 0.3s ease;
  }
}

/* ===== ADD CARD ===== */
.add-card {
  border-style: dashed;
  border-width: 2px;
  border-color: var(--border-light);
  background: var(--bg-tertiary);

  &:hover {
    border-color: var(--primary);
    background: var(--card-hover-bg);
    transform: translateY(-4px);

    .add-icon {
      background: linear-gradient(135deg, var(--primary) 0%, var(--primary-2) 100%);
      transform: rotate(90deg) scale(1.1);

      i {
        color: white;
      }
    }

    .add-text {
      color: var(--primary);
    }
  }
}

.add-card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 280px;
  gap: 1.25rem;
  padding: 2rem;
}

.add-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--card-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px var(--shadow-md);

  i {
    font-size: 2rem;
    color: var(--primary);
    transition: color 0.3s ease;
  }
}

.add-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-secondary);
  transition: color 0.3s ease;
  text-align: center;
}

.add-subtext {
  font-size: 0.875rem;
  color: var(--text-tertiary);
  text-align: center;
}
</style>
