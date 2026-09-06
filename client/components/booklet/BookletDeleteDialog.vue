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

const knowsTransactionCount = computed(() => props.transactionCount > 0)
</script>

<template>
  <ConfirmByTypingDialog
    :visible="props.visible"
    header="Supprimer ce livret ?"
    :confirmation-word="props.label"
    confirm-label="Supprimer définitivement"
    :loading="props.loading"
    @update:visible="emit('update:visible', $event)"
    @confirm="emit('confirm')"
  >
    <template v-if="knowsTransactionCount">
      Cette action est <strong>irréversible</strong> et supprimera définitivement
      <strong>{{ props.transactionCount }} transaction{{ props.transactionCount > 1 ? 's' : '' }}</strong>
      enregistrée{{ props.transactionCount > 1 ? 's' : '' }} sur ce livret.
    </template>
    <template v-else>
      Cette action est <strong>irréversible</strong> et supprimera définitivement
      <strong>toutes ses transactions</strong> enregistrées.
    </template>
  </ConfirmByTypingDialog>
</template>
