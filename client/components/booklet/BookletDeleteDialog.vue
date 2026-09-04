<script setup lang="ts">
interface Props {
  visible: boolean
  /** Label the user has to retype to confirm. */
  label: string
  /**
   * Transactions the deletion will destroy. Only announced when strictly positive: the booklet
   * list endpoint may return an empty snapshot for a booklet that does hold transactions, and
   * "0 transaction" would tell the user the deletion is harmless when it is not.
   */
  transactionCount?: number
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  transactionCount: 0,
  loading: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': []
}>()

const typedLabel = ref('')

function normalize(value: string): string {
  return value.trim().toLocaleLowerCase('fr-FR')
}

const matchesLabel = computed(() => normalize(typedLabel.value) === normalize(props.label))
const knowsTransactionCount = computed(() => props.transactionCount > 0)

// Reopening the dialog must not inherit what was typed for a previous booklet.
watch(() => props.visible, (isVisible) => {
  if (isVisible) typedLabel.value = ''
})

function cancel(): void {
  emit('update:visible', false)
}

function confirm(): void {
  if (!matchesLabel.value || props.loading) return
  emit('confirm')
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Supprimer ce livret ?"
    :style="{ width: '30rem' }"
    :breakpoints="{ '575px': '90vw' }"
    :draggable="false"
    :closable="!props.loading"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4 py-2">
      <p class="m-0 body-base">
        <template v-if="knowsTransactionCount">
          Cette action est <strong>irréversible</strong> et supprimera définitivement
          <strong>{{ props.transactionCount }} transaction{{ props.transactionCount > 1 ? 's' : '' }}</strong>
          enregistrée{{ props.transactionCount > 1 ? 's' : '' }} sur ce livret.
        </template>
        <template v-else>
          Cette action est <strong>irréversible</strong> et supprimera définitivement
          <strong>toutes ses transactions</strong> enregistrées.
        </template>
      </p>

      <div class="flex flex-col gap-2">
        <label for="confirm-booklet-label" class="text-label">
          Saisissez <strong>{{ props.label }}</strong> pour confirmer
        </label>
        <InputText
          id="confirm-booklet-label"
          v-model="typedLabel"
          data-test="confirm-label-input"
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
        data-test="cancel-delete-booklet"
        :disabled="props.loading"
        @click="cancel"
      />
      <Button
        label="Supprimer définitivement"
        icon="pi pi-trash"
        severity="danger"
        data-test="confirm-delete-booklet"
        :disabled="!matchesLabel"
        :loading="props.loading"
        @click="confirm"
      />
    </template>
  </Dialog>
</template>
