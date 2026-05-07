<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import { onMounted, ref } from 'vue'
import TagEditDialog from '~/components/dialog/TagEditDialog.vue'
import TagFormDialog from '~/components/dialog/TagFormDialog.vue'
import TagCard from '~/components/tag/TagCard.vue'
import useTag from '~/composables/useTag'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import authMiddleware from '~/middleware/auth'
import { hexToRgb } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const { addPersonalTag, addSubTag, getAllTags, deleteTag, editTag } = useTag()
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

const tags = ref<TagDisplayItem[]>([])
const addTagDialog = ref(false)
const editTagDialog = ref(false)
const tagToEdit = ref<{ id: string, label: string, colorHex: string } | null>(null)
const searchQuery = ref<string>('')
const filterType = ref<string>('all')
const selectedIds = ref<string[]>([])

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

function formattedData(tagDTO: TagDTO): TagDisplayItem {
  const color = `rgb(${tagDTO.colorDTO.red}, ${tagDTO.colorDTO.green}, ${tagDTO.colorDTO.blue})`
  return {
    id: tagDTO.tagId as string,
    label: tagDTO.label as string,
    isDefault: tagDTO.isDefault as boolean,
    color,
    parentId: tagDTO.parentId ?? null,
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

const parentTagOptions = computed(() =>
  tags.value.filter(t => !t.isDefault && !t.parentId),
)

const groupedTags = computed<TagGroupItem[]>(() => {
  const topLevel = filteredTags.value.filter(t => !t.parentId)
  return topLevel.map(parent => ({
    ...parent,
    children: filteredTags.value.filter(t => t.parentId === parent.id),
  }))
})

const visibleSelectableItems = computed<TagDisplayItem[]>(() => {
  const items: TagDisplayItem[] = []
  for (const group of groupedTags.value) {
    if (!group.isDefault) items.push(group)
    for (const child of group.children) {
      if (!child.isDefault) items.push(child)
    }
  }
  return items
})

const selectedVisibleCount = computed(() =>
  visibleSelectableItems.value.filter(t => selectedIds.value.includes(t.id)).length,
)

const isAllSelected = computed(
  () =>
    visibleSelectableItems.value.length > 0
    && visibleSelectableItems.value.every(t => selectedIds.value.includes(t.id)),
)

const isIndeterminate = computed(
  () => selectedVisibleCount.value > 0 && !isAllSelected.value,
)

function toggleSelectTag(tag: TagDisplayItem): void {
  const idx = selectedIds.value.indexOf(tag.id)
  if (idx !== -1) {
    selectedIds.value.splice(idx, 1)
  }
  else {
    selectedIds.value.push(tag.id)
  }
}

function toggleSelectAll(): void {
  if (isAllSelected.value) {
    selectedIds.value = []
  }
  else {
    selectedIds.value = visibleSelectableItems.value.map(t => t.id)
  }
}

function onBulkDeleteClick(): void {
  const toDelete = visibleSelectableItems.value.filter(t => selectedIds.value.has(t.id))
  const count = toDelete.length
  confirm.require({
    message: `Êtes-vous sûr de vouloir supprimer ${count} tag(s) sélectionné(s) ?`,
    header: 'Confirmer la suppression multiple',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: `Supprimer (${count})`,
      severity: 'danger',
    },
    accept: async () => {
      for (const tag of toDelete) {
        await performDeleteTag(tag)
      }
    },
  })
}

async function onCreateSubmit(payload: { tagLabel: string, hex: string, isSubTag: boolean, parentId: string }) {
  await withLoading(async () => {
    try {
      const rgb = hexToRgb(payload.hex)
      const colorDTO = { red: rgb.r, green: rgb.g, blue: rgb.b }
      const tag = payload.isSubTag
        ? await addSubTag(payload.tagLabel, colorDTO, payload.parentId)
        : await addPersonalTag(payload.tagLabel, colorDTO)
      tags.value.push(formattedData(tag))
      addTagDialog.value = false
      toast.success(payload.isSubTag ? 'Sous-tag créé avec succès' : 'Tag créé avec succès')
    } catch (error) {
      toast.errorAxios(error as any)
    }
  }, addTagScope)
}

async function performDeleteTag(row: TagDisplayItem, force: boolean = false): Promise<void> {
  await withLoading(async () => {
    try {
      await deleteTag(row.id, force)
      const indexDelTag = tags.value.findIndex(e => e.id === row.id)
      if (indexDelTag !== -1) {
        tags.value.splice(indexDelTag, 1)
      }
      selectedIds.value = selectedIds.value.filter(id => id !== row.id)
      toast.success('Tag supprimé avec succès')
    } catch (error: any) {
      if (error?.response?.status === 409) {
        confirm.require({
          message: 'Ce tag est utilisé dans des transactions existantes. Si vous confirmez la suppression, toutes ces transactions seront rattachées au tag par défaut "Aucune".',
          header: 'Tag utilisé dans des transactions',
          icon: 'pi pi-exclamation-triangle',
          rejectProps: {
            label: 'Annuler',
            severity: 'secondary',
            outlined: true,
          },
          acceptProps: {
            label: 'Supprimer et remplacer',
            severity: 'danger',
          },
          accept: () => performDeleteTag(row, true),
        })
      } else {
        toast.errorAxios(error)
      }
    }
  }, deleteTagScope)
}

function onDeleteClick(row: TagDisplayItem): void {
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
    accept: () => performDeleteTag(row),
  })
}

function onEditClick(row: TagDisplayItem): void {
  const rgb = row.color.match(/\d+/g)
  let colorHex = '#000000'
  if (rgb) {
    const r = Number.parseInt(rgb[0])
    const g = Number.parseInt(rgb[1] as string)
    const b = Number.parseInt(rgb[2] as string)
    colorHex = `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
  }
  tagToEdit.value = { id: row.id, label: row.label, colorHex }
  editTagDialog.value = true
}

async function applyEdit(payload: { id: string, label: string, colorHex: string }) {
  await withLoading(async () => {
    try {
      const rgb = hexToRgb(payload.colorHex)
      const tag = await editTag({
        tagId: payload.id,
        label: payload.label,
        colorDTO: {
          red: rgb.r,
          green: rgb.g,
          blue: rgb.b,
        },
        isDefault: false,
      })
      const indexTag = tags.value.findIndex(e => e.id === payload.id)
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

function onEditSubmit(payload: { id: string, label: string, colorHex: string }) {
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
    accept: () => applyEdit(payload),
  })
}
</script>

<template>
  <ConfirmDialog />
  <div class="tag-page">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          Mes Tags
        </h1>
        <p class="page-subtitle">
          Organisez vos transactions avec des catégories personnalisées
        </p>
      </div>

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

        <div v-if="visibleSelectableItems.length > 0" class="selection-controls">
          <Checkbox
            :binary="true"
            :model-value="isAllSelected"
            :indeterminate="isIndeterminate"
            v-tooltip.top="isAllSelected ? 'Désélectionner tout' : 'Tout sélectionner'"
            aria-label="Tout sélectionner"
            @update:model-value="() => toggleSelectAll()"
          />
          <span v-if="selectedVisibleCount > 0" class="selection-badge">
            {{ selectedVisibleCount }} sélectionné(s)
          </span>
          <Button
            v-if="selectedVisibleCount > 0"
            v-tooltip.top="'Supprimer la sélection'"
            icon="pi pi-trash"
            severity="danger"
            outlined
            rounded
            size="small"
            :disabled="isAnyTagActionLoading"
            :aria-label="`Supprimer ${selectedVisibleCount} tag(s) sélectionné(s)`"
            @click="onBulkDeleteClick"
          />
        </div>

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
        <TagCard
          v-for="group in groupedTags"
          :key="group.id"
          :group="group"
          :disabled="isAnyTagActionLoading"
          :deleting="isDeletingTag"
          :selected="selectedIds.includes(group.id)"
          :selected-child-ids="group.children.map(c => c.id).filter(id => selectedIds.includes(id))"
          @edit="onEditClick"
          @delete="onDeleteClick"
          @toggle-select="toggleSelectTag"
          @toggle-select-child="toggleSelectTag"
        />
      </TransitionGroup>

      <div v-if="groupedTags.length === 0" class="empty-state">
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

    <TagFormDialog
      v-model:visible="addTagDialog"
      :parent-tag-options="parentTagOptions"
      :loading="isAddingTag"
      :disabled="isAnyTagActionLoading"
      @submit="onCreateSubmit"
    />

    <TagEditDialog
      v-model:visible="editTagDialog"
      :tag="tagToEdit"
      :loading="isEditingTag"
      :disabled="isAnyTagActionLoading"
      @submit="onEditSubmit"
    />
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

.selection-controls {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.375rem 0.75rem;
  background: var(--bg-tertiary);
  border-radius: 8px;
  border: 1px solid var(--card-border);

  @media (max-width: 768px) {
    width: 100%;
    justify-content: space-between;
  }
}

.selection-badge {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
}

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

  &.header {
    margin-left: auto;
    align-self: center;
  }

  @media (max-width: 640px) {
    padding: 0.45rem;
    min-width: 44px;

    .p-button-label {
      display: none;
    }
  }
}

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
