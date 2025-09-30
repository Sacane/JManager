<script setup lang="ts">
import useDate from '~/composables/useDate'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById } = useRegularTransaction()
const transactions = ref<RegularTransactionDTO[]>([])
const { frequencyToString } = useDate()

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
  saveMonthlyTransaction(transaction)
    .then((regularTransaction: RegularTransactionDTO) => {
      console.warn('Transaction saved successfully', regularTransaction)
      isCreationDialogVisible.value = false
      transactions.value.push(regularTransaction)
    })
    .catch((err) => {
      console.error(err)
    })
}
</script>

<template>
  <div class="w-full h-full flex items-center flex-col gap-5">
    <h1>Mes transactions régulières</h1>
    <DataTable table-style="min-width: 75rem" :value="transactions">
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
    <RegularTransactionCreationDialog :visible="isCreationDialogVisible" @create-transaction="onSave" @cancel-creation="cancelCreationDialog" />
  </div>
</template>

<style scoped lang="scss">
h1 {
  color: var(--primary-3);
}
</style>
