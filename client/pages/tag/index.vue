<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import { onMounted, reactive, ref } from 'vue'
import useTag from '~/composables/useTag'
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

onMounted(() => {
  getAllTags().then((tagsData) => {
    tags.value = tagsData.map(e => formattedData(e))
  })
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

function add() {
  const rgb = hexToRgb(personalTagForm.hex)
  addPersonalTag(
    personalTagForm.tagLabel,
    {
      red: rgb.r,
      green: rgb.g,
      blue: rgb.b,
    },
  ).then((tag) => {
    tags.value.push(formattedData(tag))
    addTagDialog.value = false
    personalTagForm.tagLabel = ''
    personalTagForm.hex = '#6366f1'
  })
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
    accept: () => deleteTag(row.id).then(() => {
      const indexDelTag = tags.value.findIndex(e => e.id === row.id)
      if (indexDelTag !== -1) {
        tags.value.splice(indexDelTag, 1)
      }
    }),
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

function applyEdit() {
  const rgb = hexToRgb(tagToEdit.color)
  editTag({
    tagId: tagToEdit.id,
    label: tagToEdit.label,
    colorDTO: {
      red: rgb.r,
      green: rgb.g,
      blue: rgb.b,
    },
    isDefault: false,
  }).then((tag: TagDTO) => {
    const indexTag = tags.value.findIndex(e => e.id === tagToEdit.id)
    if (indexTag !== -1) {
      tags.value[indexTag] = formattedData(tag)
    }
    editTagDialog.value = false
  })
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
      </div>
    </div>

    <!-- Tags Grid -->
    <div class="tags-container">
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
                aria-label="Modifier"
                @click="onEditClick(tag)"
              />
              <Button
                v-tooltip.top="'Supprimer'"
                icon="pi pi-trash"
                rounded
                text
                severity="danger"
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

    <!-- Floating Action Button -->
    <Button
      v-tooltip.left="'Créer un nouveau tag'"
      icon="pi pi-plus"
      rounded
      size="large"
      class="fab-button"
      aria-label="Ajouter un tag"
      @click="addTagDialog = true"
    />

    <!-- Add Tag Dialog -->
    <Dialog
      v-model:visible="addTagDialog"
      modal
      header="Créer un nouveau tag"
      :style="{ width: '450px' }"
      class="tag-dialog"
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
          :disabled="!personalTagForm.tagLabel"
          @click="add()"
        />
      </div>
    </Dialog>

    <!-- Edit Tag Dialog -->
    <Dialog
      v-model:visible="editTagDialog"
      modal
      header="Modifier le tag"
      :style="{ width: '450px' }"
      class="tag-dialog"
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
          :disabled="!tagToEdit.label"
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
  text-transform: uppercase;
  letter-spacing: 0.5px;
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
.fab-button {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  width: 56px;
  height: 56px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3), 0 2px 4px rgba(99, 102, 241, 0.2);
  z-index: 100;
  background: #6366f1;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4), 0 4px 8px rgba(99, 102, 241, 0.3);
  }

  @media (max-width: 768px) {
    bottom: 5rem;
    right: 1rem;
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
</style>
