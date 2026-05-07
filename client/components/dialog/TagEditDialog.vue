<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import ColorPickerField from '~/components/tag/ColorPickerField.vue'

const props = defineProps<{
  tag: { id: string, label: string, colorHex: string } | null
  parentTag: { id: string, label: string } | null
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { id: string, label: string, colorHex: string, parentId: string | null }): void
}>()

const visible = defineModel<boolean>('visible', { required: true })

const form = reactive({
  label: '',
  colorHex: '#6366f1',
})

const detachFromParent = ref(false)

watch(visible, (val) => {
  if (val && props.tag) {
    form.label = props.tag.label
    form.colorHex = props.tag.colorHex
    detachFromParent.value = false
  }
}, { immediate: true })

function submit() {
  if (!props.tag) return
  const parentId = detachFromParent.value ? null : (props.parentTag?.id ?? null)
  emit('submit', { id: props.tag.id, label: form.label, colorHex: form.colorHex, parentId })
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="Modifier le tag"
    class="tag-dialog"
    :breakpoints="{ '960px': '90vw', '640px': '95vw' }"
    style="width: 450px"
  >
    <div class="dialog-content">
      <div class="form-field">
        <label for="edit-tag-label" class="form-label">
          Libellé du tag
          <span class="required">*</span>
        </label>
        <InputText
          id="edit-tag-label"
          v-model="form.label"
          class="w-full"
          autocomplete="off"
          maxlength="50"
        />
      </div>

      <div class="form-field">
        <label for="edit-tag-color" class="form-label">
          Couleur
          <span class="required">*</span>
        </label>
        <ColorPickerField
          v-model="form.colorHex"
          input-id="edit-tag-color"
        />
      </div>

      <div class="alert-box">
        <i class="pi pi-info-circle" />
        <span>La modification s'appliquera à toutes les transactions liées</span>
      </div>

      <div v-if="parentTag" class="parent-tag-section">
        <div class="parent-tag-info" :class="{ 'is-detached': detachFromParent }">
          <i class="pi pi-sitemap parent-icon" />
          <div class="parent-tag-text">
            <span class="parent-tag-label">Tag parent</span>
            <span class="parent-tag-name">{{ parentTag.label }}</span>
          </div>
          <Button
            v-if="!detachFromParent"
            label="Détacher"
            icon="pi pi-times"
            severity="secondary"
            size="small"
            outlined
            @click="detachFromParent = true"
          />
          <Button
            v-else
            label="Annuler"
            icon="pi pi-undo"
            severity="secondary"
            size="small"
            outlined
            @click="detachFromParent = false"
          />
        </div>
        <p v-if="detachFromParent" class="detach-warning">
          <i class="pi pi-exclamation-triangle" />
          Ce tag deviendra un tag indépendant après enregistrement.
        </p>
      </div>

      <Button
        label="Enregistrer les modifications"
        icon="pi pi-save"
        class="w-full mt-4"
        :loading="loading"
        :disabled="!form.label || disabled"
        @click="submit"
      />
    </div>
  </Dialog>
</template>

<style lang="scss" scoped>
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

.alert-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: #eff6ff;
  border-left: 4px solid #3b82f6;
  border-radius: 6px;
  color: #1e40af;
  font-size: 0.875rem;
}

.dark .alert-box {
  background: rgba(59, 130, 246, 0.1);
  color: #93c5fd;
}

.parent-tag-section {
  margin-bottom: 1.5rem;
}

.parent-tag-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--p-surface-100, #f3f4f6);
  border: 1px solid var(--p-surface-200, #e5e7eb);
  border-radius: 6px;
  transition: opacity 0.2s ease;
}

.parent-tag-info.is-detached {
  opacity: 0.5;
  text-decoration: line-through;
}

.dark .parent-tag-info {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.parent-icon {
  color: var(--p-primary-color);
  font-size: 1rem;
  flex-shrink: 0;
}

.parent-tag-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.parent-tag-label {
  font-size: 0.75rem;
  color: var(--p-text-muted-color, #6b7280);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.parent-tag-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--p-text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detach-warning {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: var(--p-orange-500, #f97316);
  padding: 0 0.25rem;
}

.dark .detach-warning {
  color: var(--p-orange-400, #fb923c);
}

:deep(.tag-dialog) {
  width: 450px !important;
  max-width: 95vw !important;
  box-sizing: border-box !important;
}

:deep(.tag-dialog .p-dialog-content) {
  padding: 1rem !important;
}

:deep(.tag-dialog .p-dialog-header) {
  padding: 0.75rem 1rem !important;
}

@media (max-width: 640px) {
  :deep(.tag-dialog) {
    width: 95vw !important;
    margin: 1.2rem !important;
  }

  :deep(.tag-dialog .p-dialog-content) {
    padding: 0.75rem !important;
  }

  .dialog-content {
    padding: 0.25rem 0;
  }

  .form-field {
    margin-bottom: 1rem;
  }

  .form-label {
    font-size: 0.85rem;
  }

  .p-button {
    font-size: 0.95rem;
  }
}
</style>
