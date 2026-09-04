<script setup lang="ts">
/**
 * Confirmation dialog for destructive actions, where clicking is not enough: the user has to
 * retype a word — a booklet label, their username — before the action becomes available.
 *
 * The consequence itself goes in the default slot, so each caller states what is about to be
 * destroyed in its own terms.
 */
interface Props {
  visible: boolean
  header: string
  /** Word the user must retype. Compared trimmed and case-folded. */
  confirmationWord: string
  confirmLabel: string
  /** Short instruction above the input; the word itself is appended in bold. */
  promptLabel?: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  promptLabel: 'Saisissez',
  loading: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': []
}>()

const typedWord = ref('')

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase('fr-FR')
}

const matches = computed(() => normalize(typedWord.value) === normalize(props.confirmationWord))

// Reopening for another target must not inherit what was typed for the previous one.
watch(() => props.visible, (isVisible) => {
  if (isVisible) typedWord.value = ''
})

function cancel(): void {
  emit('update:visible', false)
}

function confirm(): void {
  if (!matches.value || props.loading) return
  emit('confirm')
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    :header="props.header"
    :style="{ width: '30rem' }"
    :breakpoints="{ '575px': '90vw' }"
    :draggable="false"
    :closable="!props.loading"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4 py-2">
      <p class="m-0 body-base">
        <slot />
      </p>

      <div class="flex flex-col gap-2">
        <label for="confirm-typed-word" class="text-label">
          {{ props.promptLabel }} <strong>{{ props.confirmationWord }}</strong> pour confirmer
        </label>
        <InputText
          id="confirm-typed-word"
          v-model="typedWord"
          data-test="confirm-typed-word"
          autocomplete="off"
          :disabled="props.loading"
          maxlength="100"
        />
      </div>
    </div>

    <template #footer>
      <Button
        label="Annuler"
        icon="pi pi-times"
        severity="secondary"
        outlined
        data-test="cancel-danger-action"
        :disabled="props.loading"
        @click="cancel"
      />
      <Button
        :label="props.confirmLabel"
        icon="pi pi-trash"
        severity="danger"
        data-test="confirm-danger-action"
        :disabled="!matches"
        :loading="props.loading"
        @click="confirm"
      />
    </template>
  </Dialog>
</template>
