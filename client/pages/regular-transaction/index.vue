<script setup lang="ts">
import useDate from '~/composables/useDate'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const { fetch } = useBooklet()
const { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById, updateRegularTransaction, deleteRegularTransaction } = useRegularTransaction()
const transactions = ref<RegularTransactionDTO[]>([])
const { frequencyToString } = useDate()
const jToast = useJToast()
const booklets = ref<OnlyBookletInfo[]>([])
const selectedTransactions = ref<RegularTransactionDTO[]>([])
const isMobile = ref(false)
const confirm = useConfirm()

onMounted(() => {
  fetch()
    .then((res: BookletDTO[]) => {
      booklets.value = res.map(booklet => ({
        id: booklet.id,
        amount: booklet.amount,
        labelAccount: booklet.labelAccount,
        currency: booklet.currency,
      }))
    })
  getRegularTransaction()
    .then((res) => {
      transactions.value = res
    })
    .catch((err) => {
      console.error(err)
    })

  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
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
      jToast.success('La transaction mensuel a bien été généré')
    })
    .catch((err) => {
      console.error(err)
      jToast.errorAxios(err)
    })
}

const isEditDialogVisible = ref(false)
const selectedTransaction = ref<RegularTransactionDTO | null>(null)
const loadingTransaction = ref(false)

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

async function handleEditSave(updatedTransaction: RegularTransactionDTO) {
  try {
    const updateRequest: UpdateRegularTransactionRequest = {
      id: updatedTransaction.id,
      label: updatedTransaction.label,
      value: updatedTransaction.value,
      isIncome: updatedTransaction.isIncome,
      tagDTO: updatedTransaction.tagDTO,
      frequencyProperty: updatedTransaction.frequencyProperty,
      bookletIds: [],
      recurrenceRule: {
        type: updatedTransaction.regularity,
        value: updatedTransaction.regularity === 'MONTHLY' ? 1 : undefined,
      },
    }

    const updated = await updateRegularTransaction(updateRequest)

    const index = transactions.value.findIndex(t => t.id === updated.id)
    if (index !== -1) {
      transactions.value[index] = updated
    }

    isEditDialogVisible.value = false
    jToast.success('Transaction régulière mise à jour avec succès')
  } catch (error: any) {
    console.error('Erreur lors de la mise à jour:', error)
    jToast.errorAxios(error)
  }
}

function handleDelete(transactionId: string) {
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer cette transaction régulière ? Cette action est irréversible.',
    header: '⚠️ Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Annuler',
    acceptLabel: 'Supprimer',
    rejectProps: {
      label: 'Annuler',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Supprimer',
      severity: 'danger',
    },
    accept: async () => {
      try {
        await deleteRegularTransaction(transactionId)

        transactions.value = transactions.value.filter(t => t.id !== transactionId)
        isEditDialogVisible.value = false

        jToast.success('Transaction régulière supprimée avec succès')
      } catch (error: any) {
        console.error('Erreur lors de la suppression:', error)
        jToast.errorAxios(error)
      }
    },
    reject: () => {

    },
  })
}

function isSelected(transaction: RegularTransactionDTO): boolean {
  return selectedTransactions.value.some(t => t.id === transaction.id)
}
function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

const transactionsCount = computed(() => transactions.value.length)
</script>

<template>
  <div class="flex flex-col h-screen p-8 gap-5 bg-gradient-to-br from-gray-50 to-gray-100 md:p-2.5 md:h-auto md:min-h-screen md:gap-3.75 md:pb-7.5 lg:p-15 xl:p-10">
    <!-- Header -->
    <div class="bg-white rounded-5 p-6 shadow-lg shadow-purple-500/8 border border-purple-500/10 md:p-4 md:rounded-4">
      <div class="flex items-center gap-4">
        <div class="flex-1">
          <h1 class="text-5 font-extrabold bg-gradient-to-br from-purple-600 to-purple-800 bg-clip-text text-transparent m-0 mb-1.5 md:text-9 md:mb-2" style="background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
            💰 Mes transactions régulières
          </h1>
          <div class="flex gap-2 flex-wrap md:gap-3">
            <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-gradient-to-br from-purple-600/6 to-purple-800/6 border border-purple-500/20 rounded-full text-3 font-semibold text-purple-600 md:px-3 md:py-1.5 md:text-4">
              <i class="pi pi-sync text-2.8 md:text-3.5" />
              {{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions principales -->
    <div class="flex justify-start gap-3 md:flex-row">
      <Button
        class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25 transition-all duration-300 hover:from-purple-700 hover:to-purple-900 hover:shadow-xl hover:shadow-purple-500/35 hover:-translate-y-0.5 md:w-auto md:text-3.5 md:px-4 md:py-2.5"
        icon="pi pi-plus"
        :label="isMobile ? 'Créer' : 'Créer une transaction régulière'"
        @click="openCreationRegularTransactionDialog"
      />
    </div>

    <!-- Table des transactions (Desktop) -->
    <div v-if="!isMobile" class="flex-1 bg-white rounded-5 overflow-hidden shadow-lg shadow-purple-500/8 border border-purple-500/10">
      <DataTable
        v-model:selection="selectedTransactions"
        :value="transactions"
        scrollable
        scroll-height="flex"
        striped-rows
        @row-dblclick="handleRowDoubleClick"
      >
        <template #empty>
          <div class="flex flex-col items-center justify-center p-15 text-center md:p-10">
            <i class="pi pi-sync text-4rem text-gray-300 mb-4" />
            <h3 class="text-1.25rem font-bold text-gray-700 m-0 mb-2">
              Aucune transaction régulière
            </h3>
            <p class="text-1rem text-gray-500 m-0 mb-6">
              Commencez par créer votre première transaction régulière
            </p>
            <Button
              class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25"
              icon="pi pi-plus"
              label="Créer une transaction"
              @click="openCreationRegularTransactionDialog"
            />
          </div>
        </template>

        <Column field="label" header="Libellé" :style="{ minWidth: '200px' }">
          <template #body="{ data }">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-gray-800">{{ data.label }}</span>
            </div>
          </template>
        </Column>

        <Column field="isIncome" header="Type" :style="{ minWidth: '130px' }">
          <template #body="slotProps">
            <Tag
              :value="slotProps.data.isIncome ? 'Recette' : 'Dépense'"
              :severity="slotProps.data.isIncome ? 'success' : 'danger'"
              :icon="slotProps.data.isIncome ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"
            />
          </template>
        </Column>

        <Column field="value" header="Montant" :style="{ minWidth: '150px' }">
          <template #body="slotProps">
            <span class="font-extrabold text-1.1rem font-mono" :class="slotProps.data.isIncome ? 'text-green-500 before:content-[\'+_\']' : 'text-red-500 before:content-[\'-_\']'">
              {{ Math.abs(slotProps.data.value).toFixed(2) }} €
            </span>
          </template>
        </Column>

        <Column field="regularity" header="Fréquence" :style="{ minWidth: '130px' }">
          <template #body="slotProps">
            <div class="flex items-center gap-2 text-gray-500 font-medium">
              <i class="pi pi-clock text-0.875rem text-purple-600" />
              <span>{{ frequencyToString(slotProps.data.regularity) }}</span>
            </div>
          </template>
        </Column>

        <Column field="startDate" header="Date de début" :style="{ minWidth: '140px' }">
          <template #body="{ data }">
            <div class="flex items-center gap-2 text-gray-500 font-medium">
              <i class="pi pi-calendar text-0.875rem text-purple-600" />
              <span>{{ data.startDate }}</span>
            </div>
          </template>
        </Column>

        <Column field="tag" header="Catégorie" :style="{ minWidth: '150px' }">
          <template #body="slotProps">
            <Tag :value="slotProps.data.tagDTO.label" :style="getTagStyle(slotProps.data.tagDTO.colorDTO)" />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Liste des transactions (Mobile) -->
    <div v-else class="flex-1 overflow-hidden flex flex-col md:overflow-visible md:flex-none">
      <div v-if="transactions.length === 0" class="flex-1 flex flex-col items-center justify-center p-10 text-center bg-white rounded-4 shadow-lg shadow-purple-500/8 md:p-5">
        <i class="pi pi-sync text-3rem text-gray-300 mb-4" />
        <h3 class="text-1.125rem font-bold text-gray-700 m-0 mb-2">
          Aucune transaction régulière
        </h3>
        <p class="text-0.875rem text-gray-500 m-0 mb-5">
          Commencez par créer votre première transaction régulière
        </p>
        <Button
          class="bg-gradient-to-br from-purple-600 to-purple-800 border-none font-semibold shadow-lg shadow-purple-500/25"
          icon="pi pi-plus"
          label="Créer"
          @click="openCreationRegularTransactionDialog"
        />
      </div>

      <div v-else class="flex-1 overflow-y-auto p-1 flex flex-col gap-3 md:overflow-y-visible md:flex-none md:gap-4 md:p-0">
        <div
          v-for="transaction in transactions"
          :key="transaction.id"
          class="bg-white rounded-4 p-4 shadow-md shadow-purple-500/8 border-2 border-transparent transition-all duration-300 cursor-pointer relative overflow-hidden before:content-[''] before:absolute before:left-0 before:top-0 before:bottom-0 before:w-1 before:bg-gradient-to-b before:from-purple-600 before:to-purple-800 before:transition-width before:duration-300 hover:border-purple-500 hover:bg-gradient-to-br hover:from-purple-50 hover:to-purple-50 hover:shadow-lg hover:shadow-purple-500/20 hover:before:w-1.5 active:scale-98 md:p-4.5 md:rounded-4.5 md:shadow-lg md:shadow-purple-500/12 md:before:w-1.25"
          :class="{ 'border-purple-600 bg-gradient-to-br from-purple-50 to-purple-50 shadow-lg shadow-purple-500/20 before:w-1.5 md:shadow-xl md:shadow-purple-500/25': isSelected(transaction) }"
          @click="handleRowDoubleClick({ data: transaction })"
        >
          <!-- Header de la carte -->
          <div class="flex justify-between items-center mb-3 pb-3 border-b border-gray-100 md:mb-3.5 md:pb-3.5">
            <div class="flex items-center gap-3">
              <Tag
                :value="transaction.isIncome ? 'Recette' : 'Dépense'"
                :severity="transaction.isIncome ? 'success' : 'danger'"
                :icon="transaction.isIncome ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"
              />
            </div>
          </div>

          <!-- Corps de la carte -->
          <div class="flex flex-col gap-3 md:gap-3.5">
            <div class="flex items-center justify-between gap-2">
              <span class="text-1.1rem font-bold text-gray-800 flex-1 break-words leading-1.4 md:text-1.15rem">{{ transaction.label }}</span>
            </div>

            <div class="flex flex-col gap-2.5 md:gap-3">
              <div class="flex items-center gap-2.5 text-0.95rem text-gray-500 md:text-1rem md:gap-3">
                <i class="pi pi-euro text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span class="text-1.4rem font-extrabold font-mono md:text-1.6rem" :class="transaction.isIncome ? 'text-green-500' : 'text-red-500'">
                  {{ transaction.isIncome ? '+' : '-' }}{{ Math.abs(transaction.value).toFixed(2) }} €
                </span>
              </div>

              <div class="flex items-center gap-2.5 text-0.95rem text-gray-500 md:text-1rem md:gap-3">
                <i class="pi pi-clock text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span>{{ frequencyToString(transaction.regularity) }}</span>
              </div>

              <div class="flex items-center gap-2.5 text-0.95rem text-gray-500 md:text-1rem md:gap-3">
                <i class="pi pi-calendar text-1rem text-purple-600 w-5 text-center md:text-1.1rem md:w-5.5" />
                <span>{{ transaction.startDate }}</span>
              </div>
            </div>

            <div class="flex justify-start pt-2 border-t border-gray-100 md:pt-2.5">
              <Tag
                :value="transaction.tagDTO.label"
                :style="getTagStyle(transaction.tagDTO.colorDTO)"
                class="text-0.8rem px-3 py-1.5 md:text-0.85rem md:px-3.5 md:py-1.75"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <RegularTransactionCreationDialog
      :visible="isCreationDialogVisible"
      :booklets="booklets"
      @create-transaction="onSave"
      @cancel-creation="cancelCreationDialog"
    />

    <RegularTransactionDialogCard
      v-model="isEditDialogVisible"
      :transaction="selectedTransaction"
      :loading="loadingTransaction"
      @save="handleEditSave"
      @delete="handleDelete"
    />

    <ConfirmDialog />
  </div>
</template>

<style scoped>
:deep(.p-datatable) .p-datatable-thead > tr > th {
  background: var(--primary);
  color: white;
  font-weight: 700;
  padding: 16px;
  border: none;
  text-transform: uppercase;
  font-size: 0.875rem;
  letter-spacing: 0.05em;
}

:deep(.p-datatable) .p-datatable-tbody > tr {
  transition: all 0.2s ease;
  border-bottom: 1px solid #f3f4f6;
  background: white;
  cursor: pointer;
}

:deep(.p-datatable) .p-datatable-tbody > tr:hover {
  background: linear-gradient(135deg, #faf5ff, #f9f5ff);
}

:deep(.p-datatable) .p-datatable-tbody > tr > td {
  padding: 16px;
  border: none;
}

:deep(.p-tag) {
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.875rem;
}
</style>
