<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import { onMounted, reactive, ref } from 'vue'
import useTag from '~/composables/useTag'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import { hexToRgb } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

interface DataDisplay {
  id: string
  label: string
  isDefault: boolean
  color: string
}

const { addPersonalTag, getAllTags, deleteTag, editTag } = useTag()
const { isScopeLoading, withLoading } = useLoading()
const toast = useJToast()

const loadTagsScope = LOADING_SCOPES.tag.load
const addTagScope = LOADING_SCOPES.tag.add
const editTagScope = LOADING_SCOPES.tag.edit
const deleteTagScope = LOADING_SCOPES.tag.delete
const isLoadingTags = computed(() => isScopeLoading(loadTagsScope))
const isAddingTag = computed(() => isScopeLoading(addTagScope))
const isEditingTag = computed(() => isScopeLoading(editTagScope))
const isDeletingTag = computed(() => isScopeLoading(deleteTagScope))
const isAnyTagActionLoading = computed(() =>
  isLoadingTags.value
  || isAddingTag.value
  || isEditingTag.value
  || isDeletingTag.value,
)

const tags = ref<DataDisplay[]>([])
const addTagDialog = ref<boolean>(false)
const editTagDialog = ref<boolean>(false)
const searchQuery = ref<string>('')
const filterType = ref<string>('all') // 'all', 'default', 'personal'

const tagToEdit = reactive({
  id: '',
  label: '',
  color: '',
  isDefault: false,
})

const personalTagForm = reactive({
  tagLabel: '',
  hex: '#6366f1',
})

const confirm = useConfirm()

async function loadTags() {
  await withLoading(async () => {
    try {
      const tagsData = await getAllTags()
      tags.value = tagsData.map(e => formattedData(e))
    } catch (error) {
      toast.errorAxios(error as any)
    }
  }, loadTagsScope)
}

onMounted(() => {
  loadTags()
})

function formattedData(tagDTO: TagDTO): DataDisplay {
  const color = `rgb(${tagDTO.colorDTO.red}, ${tagDTO.colorDTO.green}, ${tagDTO.colorDTO.blue})`
  return {
    id: tagDTO.tagId as string,
    label: tagDTO.label as string,
    isDefault: tagDTO.isDefault as boolean,
    color,
  }
}

const filteredTags = computed(() => {
  let filtered = tags.value

  // Filter by type
  if (filterType.value === 'default') {
    filtered = filtered.filter(t => t.isDefault)
  } else if (filterType.value === 'personal') {
    filtered = filtered.filter(t => !t.isDefault)
  }

  // Filter by search
  if (searchQuery.value) {
    filtered = filtered.filter(t =>
      t.label.toLowerCase().includes(searchQuery.value.toLowerCase()),
    )
  }

  return filtered
})

async function add() {
  await withLoading(async () => {
    try {
      const rgb = hexToRgb(personalTagForm.hex)
      const tag = await addPersonalTag(
        personalTagForm.tagLabel,
        {
          red: rgb.r,
          green: rgb.g,
          blue: rgb.b,
        },
      )
      tags.value.push(formattedData(tag))
      addTagDialog.value = false
      personalTagForm.tagLabel = ''
      personalTagForm.hex = '#6366f1'
      toast.success('Tag créé avec succès')
    } catch (error) {
      toast.errorAxios(error as any)
    }
  }, addTagScope)
}

function onDeleteClick(row: DataDisplay): void {
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ce tag ?',
    header: 'Confirmer la suppression du tag',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Supprimer',
      severity: 'danger',
    },
    accept: async () => {
      await withLoading(async () => {
        try {
          await deleteTag(row.id)
          const indexDelTag = tags.value.findIndex(e => e.id === row.id)
          if (indexDelTag !== -1) {
            tags.value.splice(indexDelTag, 1)
          }
          toast.success('Tag supprimé avec succès')
        } catch (error) {
          toast.errorAxios(error as any)
        }
      }, deleteTagScope)
    },
  })
}

function onEditClick(row: DataDisplay): void {
  editTagDialog.value = true
  tagToEdit.label = row.label
  tagToEdit.id = row.id
  const rgb = row.color.match(/\d+/g)
  if (rgb) {
    const red = Number.parseInt(rgb[0])
    const green = Number.parseInt(rgb[1] as string)
    const blue = Number.parseInt(rgb[2] as string)
    tagToEdit.color = `#${((1 << 24) + (red << 16) + (green << 8) + blue).toString(16).slice(1)}`
  }
}

async function applyEdit() {
  await withLoading(async () => {
    try {
      const rgb = hexToRgb(tagToEdit.color)
      const tag = await editTag({
        tagId: tagToEdit.id,
        label: tagToEdit.label,
        colorDTO: {
          red: rgb.r,
          green: rgb.g,
          blue: rgb.b,
        },
        isDefault: false,
      })
      const indexTag = tags.value.findIndex(e => e.id === tagToEdit.id)
      if (indexTag !== -1) {
        tags.value[indexTag] = formattedData(tag)
      }
      editTagDialog.value = false
      toast.success('Tag modifié avec succès')
    } catch (error) {
      toast.errorAxios(error as any)
    }
  }, editTagScope)
}

function edit() {
  confirm.require({
    message: 'Si vous modifiez ce tag, toutes vos transactions rattachées à ce tag seront modifiées. Voulez-vous continuer ?',
    header: 'Confirmation de modification',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Continuer',
    },
    accept: () => applyEdit(),
  })
}
</script>

<template>
  <ConfirmDialog />
  <div class="tag-page">
    <!-- Header Section -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          Mes Tags
        </h1>
        <p class="page-subtitle">
          Organisez vos transactions avec des catégories personnalisées
        </p>
      </div>

      <!-- Search and Filter Bar -->
      <div class="toolbar">
        <div class="search-box">
          <i class="pi pi-search search-icon" />
          <InputText
            v-model="searchQuery"
            placeholder="Rechercher un tag..."
            class="search-input"
          />
        </div>

        <SelectButton
          v-model="filterType"
          :options="[
            { label: 'Tous', value: 'all' },
            { label: 'Par défaut', value: 'default' },
            { label: 'Personnels', value: 'personal' },
          ]"
          option-label="label"
          option-value="value"
          class="filter-buttons"
        />

        <!-- Modern action button moved into toolbar and aligned right -->
        <Button
          v-tooltip.left="'Créer un nouveau tag'"
          icon="pi pi-plus"
          label="Nouveau tag"
          class="modern-fab header"
          :disabled="isAnyTagActionLoading"
          aria-label="Ajouter un tag"
          @click="addTagDialog = true"
        />
      </div>
    </div>

    <!-- Tags Grid -->
    <div class="tags-container">
      <div v-if="isLoadingTags" class="loading-container">
        <ProgressSpinner
          style="width: 48px; height: 48px"
          stroke-width="4"
        />
        <p class="loading-text">
          Chargement des tags...
        </p>
      </div>
      <TransitionGroup name="tag-list" tag="div" class="tags-grid">
        <div
          v-for="tag in filteredTags"
          :key="tag.id"
          class="tag-card"
          :class="{ 'tag-card-personal': !tag.isDefault }"
          :style="{ '--tag-color': tag.color }"
        >
          <!-- Color Band -->
          <div class="tag-color-band" :style="{ backgroundColor: tag.color }" />

          <!-- Card Content -->
          <div class="tag-content">
            <div class="tag-info">
              <div class="tag-label-wrapper">
                <h3 class="tag-label">
                  {{ tag.label }}
                </h3>
                <Tag
                  :value="tag.isDefault ? 'Par défaut' : 'Personnel'"
                  :severity="tag.isDefault ? 'info' : 'success'"
                  class="tag-badge"
                />
              </div>

              <!-- Color Preview -->
              <div class="color-preview">
                <div class="color-circle" :style="{ backgroundColor: tag.color }" />
                <span class="color-label">{{ tag.color }}</span>
              </div>
            </div>

            <!-- Actions (only for personal tags) -->
            <div v-if="!tag.isDefault" class="tag-actions">
              <Button
                v-tooltip.top="'Modifier'"
                icon="pi pi-pencil"
                rounded
                text
                severity="secondary"
                :disabled="isAnyTagActionLoading"
                aria-label="Modifier"
                @click="onEditClick(tag)"
              />
              <Button
                v-tooltip.top="'Supprimer'"
                icon="pi pi-trash"
                rounded
                text
                severity="danger"
                :loading="isDeletingTag"
                :disabled="isAnyTagActionLoading"
                aria-label="Supprimer"
                @click="onDeleteClick(tag)"
              />
            </div>
          </div>
        </div>
      </TransitionGroup>

      <!-- Empty State -->
      <div v-if="filteredTags.length === 0" class="empty-state">
        <i class="pi pi-tag empty-icon" />
        <h3>Aucun tag trouvé</h3>
        <p v-if="searchQuery">
          Aucun tag ne correspond à votre recherche
        </p>
        <p v-else>
          Commencez par créer votre premier tag personnel
        </p>
      </div>
    </div>

    <Dialog
      v-model:visible="addTagDialog"
      modal
      header="Créer un nouveau tag"
      class="tag-dialog"
      :breakpoints="{ '960px': '90vw', '640px': '95vw' }"
      style="width:450px"
    >
      <div class="dialog-content">
        <div class="form-field">
          <label for="tag-label" class="form-label">
            Libellé du tag
            <span class="required">*</span>
          </label>
          <InputText
            id="tag-label"
            v-model="personalTagForm.tagLabel"
            placeholder="Ex: Courses, Essence, Loisirs..."
            class="w-full"
            autocomplete="off"
          />
        </div>

        <div class="form-field">
          <label for="tag-color" class="form-label">
            Couleur
            <span class="required">*</span>
          </label>
          <div class="color-picker-wrapper">
            <input
              id="tag-color"
              v-model="personalTagForm.hex"
              type="color"
              class="color-picker"
            >
            <InputText
              v-model="personalTagForm.hex"
              class="color-hex-input"
              placeholder="#000000"
            />
            <div class="color-preview-large" :style="{ backgroundColor: personalTagForm.hex }" />
          </div>
        </div>

        <Button
          label="Créer le tag"
          icon="pi pi-check"
          class="w-full mt-4"
          :loading="isAddingTag"
          :disabled="!personalTagForm.tagLabel || isAnyTagActionLoading"
          @click="add()"
        />
      </div>
    </Dialog>

    <!-- Edit Tag Dialog -->
    <Dialog
      v-model:visible="editTagDialog"
      modal
      header="Modifier le tag"
      class="tag-dialog"
      :breakpoints="{ '960px': '90vw', '640px': '95vw' }"
      style="width:450px"
    >
      <div class="dialog-content">
        <div class="form-field">
          <label for="edit-tag-label" class="form-label">
            Libellé du tag
            <span class="required">*</span>
          </label>
          <InputText
            id="edit-tag-label"
            v-model="tagToEdit.label"
            class="w-full"
            autocomplete="off"
          />
        </div>

        <div class="form-field">
          <label for="edit-tag-color" class="form-label">
            Couleur
            <span class="required">*</span>
          </label>
          <div class="color-picker-wrapper">
            <input
              id="edit-tag-color"
              v-model="tagToEdit.color"
              type="color"
              class="color-picker"
            >
            <InputText
              v-model="tagToEdit.color"
              class="color-hex-input"
              placeholder="#000000"
            />
            <div class="color-preview-large" :style="{ backgroundColor: tagToEdit.color }" />
          </div>
        </div>

        <div class="alert-box">
          <i class="pi pi-info-circle" />
          <span>La modification s'appliquera à toutes les transactions liées</span>
        </div>

        <Button
          label="Enregistrer les modifications"
          icon="pi pi-save"
          class="w-full mt-4"
          :loading="isEditingTag"
          :disabled="!tagToEdit.label || isAnyTagActionLoading"
          @click="edit()"
        />
      </div>
    </Dialog>
  </div>
</template>

<style lang="scss" scoped>
.tag-page {
  min-height: 100vh;
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, var(--bg-gradient-from) 0%, var(--bg-gradient-to) 100%);

  @media (max-width: 768px) {
    padding: 1rem;
    margin-top: 4rem;
  }
}

/* Header */
.page-header {
  margin-bottom: 2rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1.25rem 1rem;
  margin-bottom: 1rem;
  border-radius: 14px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
}

.loading-text {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 500;
}

.header-content {
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  color: var(--text-primary);
  letter-spacing: -0.025em;
}

.page-subtitle {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  font-weight: 400;
}

/* Toolbar */
.toolbar {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;
  }
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 250px;

  @media (max-width: 768px) {
    min-width: 100%;
  }
}

.search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  z-index: 1;
}

.search-input {
  width: 100%;
  padding-left: 2.75rem;
}

.filter-buttons {
  @media (max-width: 768px) {
    width: 100%;
    display: flex;

    :deep(.p-button) {
      flex: 1;
    }
  }
}

/* Tags Grid */
.tags-container {
  position: relative;
  min-height: 400px;
}

.tags-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

/* Tag Card */
.tag-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px var(--shadow-sm), 0 1px 2px var(--shadow-sm);
  transition: all 0.3s ease;
  border: 1px solid var(--card-border);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px var(--shadow-md), 0 4px 8px var(--shadow-sm);
    border-color: var(--border-light);
  }

  &.tag-card-personal:hover {
    .tag-actions {
      opacity: 1;
      pointer-events: all;
    }
  }
}

.tag-color-band {
  height: 6px;
  width: 100%;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.2) 100%);
  }
}

.tag-content {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.tag-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.tag-label-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag-label {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.tag-badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}

/* Color Preview */
.color-preview {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.color-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  box-shadow: 0 2px 8px var(--shadow-sm);
  flex-shrink: 0;
}

.color-label {
  font-family: 'SF Mono', 'Monaco', 'Consolas', 'Courier New', monospace;
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  font-weight: 500;
}

/* Tag Actions */
.tag-actions {
  display: flex;
  gap: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-color);
  opacity: 0.7;
  transition: opacity 0.3s ease;

  @media (max-width: 768px) {
    opacity: 1;
  }
}

/* FAB Button */
.modern-fab {
  border-radius: 999px;
  padding: 0.5rem 1rem;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  background: linear-gradient(90deg, rgba(99,102,241,1) 0%, rgba(79,70,229,1) 100%);
  color: white;
  box-shadow: 0 6px 14px rgba(79,70,229,0.18), 0 2px 6px rgba(16,24,40,0.06);
  border: 1px solid rgba(255,255,255,0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
  min-height: 44px;

  .p-button-icon {
    font-size: 1.05rem;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 24px rgba(79,70,229,0.22), 0 4px 10px rgba(16,24,40,0.09);
  }

  &:active {
    transform: translateY(0);
  }

  /* Header variant: align to right inside toolbar */
  &.header {
    margin-left: auto;
    align-self: center;
  }

  @media (max-width: 640px) {
    padding: 0.45rem;
    min-width: 44px;

    /* Hide the label on small screens to remain compact */
    .p-button-label {
      display: none;
    }
  }
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 4rem;
  color: var(--text-muted);
  opacity: 0.6;
  margin-bottom: 1rem;
}

.empty-state h3 {
  font-size: 1.25rem;
  margin: 0 0 0.5rem 0;
  color: var(--text-primary);
  font-weight: 600;
}

.empty-state p {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-secondary);
}

/* Dialog Styles */
.dialog-content {
  padding: 1rem 0;
}

.form-field {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.875rem;
  letter-spacing: 0.01em;
}

.required {
  color: #ef4444;
  margin-left: 0.25rem;
}

.color-picker-wrapper {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.color-picker {
  width: 60px;
  height: 42px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  background: none;

  &::-webkit-color-swatch-wrapper {
    padding: 2px;
  }

  &::-webkit-color-swatch {
    border: none;
    border-radius: 4px;
  }
}

.color-hex-input {
  flex: 1;
}

.color-preview-large {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  border: 2px solid var(--border-color);
  box-shadow: 0 2px 8px var(--shadow-sm);
  flex-shrink: 0;
}

.alert-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: #eff6ff;
  border-left: 4px solid #3b82f6;
  border-radius: 6px;
  color: #1e40af;
  font-size: 0.875rem
}

.dark .alert-box {
  background: rgba(59, 130, 246, 0.1);
  color: #93c5fd;
}

/* Animations */
.tag-list-move,
.tag-list-enter-active,
.tag-list-leave-active {
  transition: all 0.3s ease;
}

.tag-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.tag-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.tag-list-leave-active {
  position: absolute;
}

/* Dialog sizing and responsive adjustments for PrimeVue Dialog */
:deep(.tag-dialog) {
  /* PrimeVue may apply inline styles; ensure our size rules are prioritized */
  width: 450px !important;
  max-width: 95vw !important;
  box-sizing: border-box !important;
}

/* Content inside the dialog */
:deep(.tag-dialog .p-dialog-content) {
  padding: 1rem !important;
}

/* Titlebar tweaks */
:deep(.tag-dialog .p-dialog-header) {
  padding: 0.75rem 1rem !important;
}

/* Make form elements more compact on small screens */
@media (max-width: 640px) {
  :deep(.tag-dialog) {
    width: 95vw !important;
    margin: 1.2rem !important;
  }

  :deep(.tag-dialog .p-dialog-content) {
    padding: 0.75rem !important;
  }

  .dialog-content {
    padding: 0.25rem 0 !important;
  }

  .form-field {
    margin-bottom: 1rem;
  }

  .color-picker {
    width: 48px !important;
    height: 36px !important;
  }

  .color-preview-large {
    width: 36px !important;
    height: 36px !important;
  }

  .form-label {
    font-size: 0.85rem !important;
  }

  .p-button {
    font-size: 0.95rem !important;
  }
}
</style>
