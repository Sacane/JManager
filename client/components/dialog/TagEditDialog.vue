<script setup lang="ts">
import { reactive, watch } from 'vue'
import ColorPickerField from '~/components/tag/ColorPickerField.vue'

const props = defineProps<{
  tag: { id: string, label: string, colorHex: string } | null
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { id: string, label: string, colorHex: string }): void
}>()

const visible = defineModel<boolean>('visible', { required: true })

const form = reactive({
  label: '',
  colorHex: '#6366f1',
})

watch(visible, (val) => {
  if (val && props.tag) {
    form.label = props.tag.label
    form.colorHex = props.tag.colorHex
  }
})

function submit() {
  if (!props.tag) return
  emit('submit', { id: props.tag.id, label: form.label, colorHex: form.colorHex })
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
