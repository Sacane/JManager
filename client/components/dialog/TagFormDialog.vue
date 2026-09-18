<script setup lang="ts">
import { reactive, watch } from 'vue'
import ColorPickerField from '~/components/tag/ColorPickerField.vue'

defineProps<{
  parentTagOptions: TagDisplayItem[]
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { tagLabel: string, hex: string, isSubTag: boolean, parentId: string }): void
}>()

const visible = defineModel<boolean>('visible', { required: true })

const form = reactive({
  tagLabel: '',
  hex: '#6366f1',
  isSubTag: false,
  parentId: '' as string,
})

const fieldErrors = reactive<{ tagLabel: string | null, parentId: string | null }>({ tagLabel: null, parentId: null })

function labelErrorFor(label: string): string | null {
  return label.trim() === '' ? 'Indiquez un libellé' : null
}

function parentErrorFor(isSubTag: boolean, parentId: string): string | null {
  return isSubTag && !parentId ? 'Choisissez le tag parent de ce sous-tag' : null
}

function reset() {
  form.tagLabel = ''
  form.hex = '#6366f1'
  form.isSubTag = false
  form.parentId = ''
  fieldErrors.tagLabel = null
  fieldErrors.parentId = null
}

watch(visible, (val) => {
  if (!val) reset()
})

watch(() => form.tagLabel, (label) => {
  if (fieldErrors.tagLabel) fieldErrors.tagLabel = labelErrorFor(label)
})

watch(() => [form.isSubTag, form.parentId] as const, ([isSubTag, parentId]) => {
  if (fieldErrors.parentId) fieldErrors.parentId = parentErrorFor(isSubTag, parentId)
})

function submit() {
  fieldErrors.tagLabel = labelErrorFor(form.tagLabel)
  fieldErrors.parentId = parentErrorFor(form.isSubTag, form.parentId)

  if (fieldErrors.tagLabel || fieldErrors.parentId) return
  emit('submit', { ...form })
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="Créer un nouveau tag"
    class="tag-dialog"
    :breakpoints="{ '960px': '90vw', '640px': '95vw' }"
    style="width: 450px"
  >
    <div class="dialog-content">
      <div class="form-field">
        <label class="form-label">Type</label>
        <SelectButton
          v-model="form.isSubTag"
          :options="[
            { label: 'Tag', value: false },
            { label: 'Sous-tag', value: true },
          ]"
          option-label="label"
          option-value="value"
          class="w-full"
          data-test="subtag-toggle"
        />
      </div>

      <div v-if="form.isSubTag" class="form-field">
        <label for="parent-tag" class="form-label">
          Tag parent
          <span class="required">*</span>
        </label>
        <Select
          id="parent-tag"
          v-model="form.parentId"
          :options="parentTagOptions"
          option-label="label"
          option-value="id"
          placeholder="Sélectionner un tag parent"
          class="w-full"
          data-test="parent-tag-select"
        >
          <template #option="{ option }">
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: option.color }" />
              <span>{{ option.label }}</span>
            </div>
          </template>
        </Select>
        <FieldError data-test="error-tag-parent" :message="fieldErrors.parentId" />
      </div>

      <div class="form-field">
        <label for="tag-label" class="form-label">
          Libellé du tag
          <span class="required">*</span>
        </label>
        <InputText
          id="tag-label"
          v-model="form.tagLabel"
          placeholder="Ex: Courses, Essence, Loisirs..."
          class="w-full"
          autocomplete="off"
          maxlength="50"
          :invalid="!!fieldErrors.tagLabel"
          aria-describedby="tag-label-error"
        />
        <FieldError id="tag-label-error" data-test="error-tag-label" :message="fieldErrors.tagLabel" />
      </div>

      <div class="form-field">
        <label for="tag-color" class="form-label">
          Couleur
          <span class="required">*</span>
        </label>
        <ColorPickerField
          v-model="form.hex"
          input-id="tag-color"
        />
      </div>

      <Button
        label="Créer le tag"
        icon="pi pi-check"
        class="w-full mt-4"
        :loading="loading"
        :disabled="disabled"
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
