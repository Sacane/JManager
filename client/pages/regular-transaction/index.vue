<script setup lang="ts">
import useDate from '~/composables/useDate'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById } = useRegularTransaction()
const transactions = ref<RegularTransactionDTO[]>([])
const { frequencyToString } = useDate()
const jToast = useJToast()

onMounted(() => {
  getRegularTransaction()
    .then((res) => {
      transactions.value = res
    })
    .catch((err) => {
      console.error(err)
    })
})

const isCreationDialogVisible = ref(false)

function openCreationRegularTransactionDialog() {
  isCreationDialogVisible.value = true
}
function cancelCreationDialog() {
  isCreationDialogVisible.value = false
}

function onSave(transaction: MonthlyTransactionCreationRequest) {
  console.warn('attempt to save transaction', transaction)
  saveMonthlyTransaction(transaction)
    .then((regularTransaction: RegularTransactionDTO) => {
      console.warn('Transaction saved successfully', regularTransaction)
      isCreationDialogVisible.value = false
      transactions.value.push(regularTransaction)
      jToast.success('La transaction mensuel a bien été généré')
    })
    .catch((err) => {
      console.error(err)
      jToast.errorAxios(err)
    })
}

// Gestion du dialog d'édition
const isEditDialogVisible = ref(false)
const selectedTransaction = ref<RegularTransactionDTO | null>(null)
const loadingTransaction = ref(false)

// Gestionnaire de double-clic sur une ligne
async function handleRowDoubleClick(event: any) {
  const transactionId = event.data.id

  if (!transactionId) {
    console.error('ID de transaction manquant')
    return
  }

  loadingTransaction.value = true
  isEditDialogVisible.value = true

  try {
    selectedTransaction.value = await getRegularTransactionById(transactionId)
  } catch (error) {
    console.error('Erreur lors de la récupération de la transaction:', error)
    isEditDialogVisible.value = false
  } finally {
    loadingTransaction.value = false
  }
}

// Sauvegarder les modifications
function handleEditSave(updatedTransaction: RegularTransactionDTO) {
  // TODO: Implémenter la méthode updateRegularTransaction dans useRegularTransaction
  console.log('Transaction mise à jour:', updatedTransaction)

  // Mettre à jour la liste localement
  const index = transactions.value.findIndex(t => t.id === updatedTransaction.id)
  if (index !== -1) {
    transactions.value[index] = updatedTransaction
  }

  isEditDialogVisible.value = false
}
</script>

<template>
  <div class="w-full h-full flex items-center flex-col gap-5">
    <h1>Mes transactions régulières</h1>
    <DataTable
      table-style="min-width: 75rem"
      :value="transactions"
      @row-dblclick="handleRowDoubleClick"
    >
      <Column field="label" header="Libellé" />

      <Column field="value" header="Montant" />
      <Column field="regularity" header="Fréquence">
        <template #body="slotProps">
          {{ frequencyToString(slotProps.data.regularity) }}
        </template>
      </Column>
      <Column field="startDate" header="Date de début" />
      <Column field="tag" header="Tag">
        <template #body="slotProps">
          <Tag :value="slotProps.data.tagDTO.label" :style="getTagStyle(slotProps.data.tagDTO.colorDTO)" />
        </template>
      </Column>
    </DataTable>
    <Button label="Créer une transaction régulière" icon="pi pi-plus" @click="openCreationRegularTransactionDialog" />

    <RegularTransactionCreationDialog
      :visible="isCreationDialogVisible"
      @create-transaction="onSave"
      @cancel-creation="cancelCreationDialog"
    />

    <RegularTransactionDialogCard
      v-model="isEditDialogVisible"
      :transaction="selectedTransaction"
      :loading="loadingTransaction"
      @save="handleEditSave"
    />
  </div>
</template>

<style scoped lang="scss">
h1 {
  color: var(--primary-3);
}
</style>
