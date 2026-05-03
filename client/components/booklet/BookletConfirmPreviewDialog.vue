<script setup lang="ts">
interface Props {
  visible: boolean
  transaction: TransactionResultDTO | null
  loading: boolean
  newAmount: number | null
  newDate: Date | null
}

defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  'update:newAmount': [val: number | null]
  'update:newDate': [val: Date | null]
  'confirm': []
}>()
</script>

<template>
  <Dialog
    :visible="visible"
    modal
    header="Valider la transaction prévisionnelle"
    :style="{ width: '25rem' }"
    :pt="{
      root: { class: 'preview-confirm-root' },
      mask: { class: 'preview-confirm-mask' },
      header: { class: 'preview-confirm-header' },
      title: { class: 'preview-confirm-title' },
      closeButton: { class: 'preview-confirm-close-btn' },
      content: { class: 'preview-confirm-content' },
      footer: { class: 'preview-confirm-footer' },
    }"
    @update:visible="emit('update:visible', $event)"
  >
    <div v-if="transaction" class="flex flex-col gap-4">
      <p>Voulez-vous valider cette transaction prévisionnelle ?</p>

      <div class="preview-confirm-summary p-3 rounded-lg border">
        <div class="flex justify-between items-center text-sm">
          <span class="font-semibold">Transaction</span>
          <span class="font-medium">{{ transaction.label }}</span>
        </div>
        <div class="preview-confirm-summary-row flex justify-between items-center mt-2 pt-2 border-t">
          <span class="font-semibold">Montant de base</span>
          <span class="font-bold text-lg" :class="transaction.isIncome ? 'text-emerald-500' : 'text-red-500'">
            {{ transaction.isIncome ? '+' : '-' }} {{ transaction.value }} €
          </span>
        </div>
        <div class="preview-confirm-summary-row flex justify-between items-center mt-2 pt-2 border-t">
          <span class="font-semibold">Date de base</span>
          <span class="font-medium">{{ transaction.date }}</span>
        </div>
      </div>

      <p class="text-sm preview-confirm-help-text">
        Vous pouvez optionnellement spécifier un nouveau montant et une nouvelle date ci-dessous.
      </p>
      <div class="flex flex-col gap-3">
        <div class="flex flex-col gap-2">
          <label for="newAmount" class="font-semibold">Nouveau montant</label>
          <InputNumber
            id="newAmount"
            :model-value="newAmount"
            mode="currency"
            currency="EUR"
            locale="fr-FR"
            placeholder="0.00"
            class="preview-confirm-field"
            @update:model-value="emit('update:newAmount', $event)"
          />
        </div>
        <div class="flex flex-col gap-2">
          <label for="newDate" class="font-semibold">Nouvelle date</label>
          <DatePicker
            id="newDate"
            :model-value="newDate"
            date-format="dd/mm/yy"
            show-icon
            icon-display="input"
            class="preview-confirm-field"
            @update:model-value="emit('update:newDate', $event)"
          />
        </div>
      </div>
    </div>
    <div class="flex justify-end gap-2 mt-6">
      <Button
        type="button"
        label="Annuler"
        severity="secondary"
        :disabled="loading"
        @click="emit('update:visible', false)"
      />
      <Button
        type="button"
        label="Valider"
        :loading="loading"
        :disabled="loading"
        @click="emit('confirm')"
      />
    </div>
  </Dialog>
</template>

<style scoped lang="scss">
.preview-confirm-summary {
  background: var(--bg-tertiary);
  border-color: var(--card-border);
  color: var(--text-primary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  .preview-confirm-summary {
    background: linear-gradient(
      145deg,
      color-mix(in oklab, var(--bg-tertiary) 92%, #ffffff 8%),
      color-mix(in oklab, var(--bg-tertiary) 84%, var(--primary) 16%)
    );
    border-color: color-mix(in oklab, var(--card-border) 65%, var(--primary) 35%);
  }
}

.preview-confirm-summary-row {
  border-color: var(--border-color);
}

.preview-confirm-help-text {
  color: var(--text-secondary);
}

:deep(.preview-confirm-field .p-inputtext),
:deep(.preview-confirm-field .p-inputnumber-input),
:deep(.preview-confirm-field .p-datepicker-input) {
  background: var(--bg-secondary) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border-color) !important;
}

:deep(.preview-confirm-field .p-inputtext::placeholder),
:deep(.preview-confirm-field .p-inputnumber-input::placeholder) {
  color: var(--text-tertiary) !important;
}

:deep(.preview-confirm-field.p-inputwrapper-focus .p-inputtext),
:deep(.preview-confirm-field .p-inputtext:focus),
:deep(.preview-confirm-field .p-inputnumber-input:focus) {
  border-color: var(--primary) !important;
  box-shadow: 0 0 0 0.16rem color-mix(in oklab, var(--primary) 24%, transparent) !important;
}

.dark :deep(.preview-confirm-field .p-inputtext),
.dark :deep(.preview-confirm-field .p-inputnumber-input),
.dark :deep(.preview-confirm-field .p-datepicker-input) {
  background: #111827 !important;
  color: #f3f4f6 !important;
  border-color: #4b5563 !important;
}

.dark :deep(.preview-confirm-field .p-inputtext::placeholder),
.dark :deep(.preview-confirm-field .p-inputnumber-input::placeholder) {
  color: #9ca3af !important;
}
</style>

<!-- Global styles for PrimeVue Dialog passthrough classes -->
<style lang="scss">
.preview-confirm-root {
  background: var(--card-bg) !important;
  border: 1px solid var(--card-border) !important;
  color: var(--text-primary) !important;
  overflow: hidden;
  box-shadow: 0 18px 42px var(--shadow-lg), 0 8px 22px var(--shadow-purple) !important;
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  .preview-confirm-root {
    background: linear-gradient(
      165deg,
      color-mix(in oklab, var(--card-bg) 93%, #ffffff 7%),
      color-mix(in oklab, var(--card-bg) 86%, var(--primary) 14%)
    ) !important;
    border-color: color-mix(in oklab, var(--card-border) 55%, var(--primary) 45%) !important;
  }
}

.preview-confirm-mask {
  backdrop-filter: blur(2px);
  background: rgba(15, 23, 42, 0.32) !important;
}

.preview-confirm-header,
.preview-confirm-content,
.preview-confirm-footer {
  color: var(--text-primary) !important;
}

.preview-confirm-header {
  background: var(--bg-tertiary) !important;
  border-bottom: 1px solid var(--card-border) !important;
}

@supports (background: color-mix(in oklab, #000 0%, #fff 0%)) {
  .preview-confirm-header {
    background: linear-gradient(
      180deg,
      color-mix(in oklab, var(--bg-tertiary) 90%, #ffffff 10%),
      color-mix(in oklab, var(--bg-tertiary) 84%, var(--primary) 16%)
    ) !important;
  }
}

.preview-confirm-content,
.preview-confirm-footer {
  background: transparent !important;
}

.preview-confirm-title,
.preview-confirm-content p,
.preview-confirm-content label {
  color: var(--text-primary) !important;
}

.preview-confirm-close-btn {
  color: var(--text-secondary) !important;
  border: 1px solid var(--card-border) !important;
  background: var(--bg-secondary) !important;
  transition: all 0.2s ease;
}

.preview-confirm-close-btn:hover {
  color: var(--primary) !important;
  border-color: var(--primary) !important;
  background: var(--card-hover-bg) !important;
}

.dark .preview-confirm-root {
  background: #111827 !important;
  border-color: #374151 !important;
  color: #f3f4f6 !important;
}

.dark .preview-confirm-header,
.dark .preview-confirm-content,
.dark .preview-confirm-footer {
  background: #111827 !important;
  color: #f3f4f6 !important;
}

.dark .preview-confirm-header {
  border-bottom-color: #374151 !important;
}

.dark .preview-confirm-title,
.dark .preview-confirm-content p,
.dark .preview-confirm-content label {
  color: #f3f4f6 !important;
}

.dark .preview-confirm-close-btn {
  color: #9ca3af !important;
}

.dark .preview-confirm-close-btn:hover {
  background: #1f2937 !important;
  color: #f3f4f6 !important;
}

.dark .preview-confirm-mask {
  background: rgba(2, 6, 23, 0.58) !important;
}
</style>
