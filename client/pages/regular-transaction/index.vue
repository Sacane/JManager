<script setup lang="ts">
import useDate from '~/composables/useDate'
import { getTagStyle } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

const { fetch } = useBooklet()
const { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById } = useRegularTransaction()
const transactions = ref<RegularTransactionDTO[]>([])
const { frequencyToString } = useDate()
const jToast = useJToast()
const booklets = ref<OnlyBookletInfo[]>([])
const selectedTransactions = ref<RegularTransactionDTO[]>([])
const isMobile = ref(false)

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

function handleEditSave(updatedTransaction: RegularTransactionDTO) {
  const index = transactions.value.findIndex(t => t.id === updatedTransaction.id)
  if (index !== -1) {
    transactions.value[index] = updatedTransaction
  }

  isEditDialogVisible.value = false
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
  <div class="regular-transactions-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-top">
        <div class="page-title">
          <h1>💰 Mes transactions régulières</h1>
          <div class="page-stats">
            <span class="stat-badge">
              <i class="pi pi-sync" />
              {{ transactionsCount }} transaction{{ transactionsCount > 1 ? 's' : '' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions principales -->
    <div class="main-actions">
      <Button
        class="btn-primary"
        icon="pi pi-plus"
        :label="isMobile ? 'Créer' : 'Créer une transaction régulière'"
        @click="openCreationRegularTransactionDialog"
      />
    </div>

    <!-- Table des transactions (Desktop) -->
    <div v-if="!isMobile" class="transactions-table">
      <DataTable
        v-model:selection="selectedTransactions"
        :value="transactions"
        scrollable
        scroll-height="flex"
        striped-rows
        @row-dblclick="handleRowDoubleClick"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-sync" />
            <h3>Aucune transaction régulière</h3>
            <p>Commencez par créer votre première transaction régulière</p>
            <Button
              class="btn-primary"
              icon="pi pi-plus"
              label="Créer une transaction"
              @click="openCreationRegularTransactionDialog"
            />
          </div>
        </template>

        <Column field="label" header="Libellé" :style="{ minWidth: '200px' }">
          <template #body="{ data }">
            <div class="label-cell">
              <span class="transaction-label">{{ data.label }}</span>
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
            <span class="amount" :class="slotProps.data.isIncome ? 'income' : 'expense'">
              {{ Math.abs(slotProps.data.value).toFixed(2) }} €
            </span>
          </template>
        </Column>

        <Column field="regularity" header="Fréquence" :style="{ minWidth: '130px' }">
          <template #body="slotProps">
            <div class="frequency-cell">
              <i class="pi pi-clock" />
              <span>{{ frequencyToString(slotProps.data.regularity) }}</span>
            </div>
          </template>
        </Column>

        <Column field="startDate" header="Date de début" :style="{ minWidth: '140px' }">
          <template #body="{ data }">
            <div class="date-cell">
              <i class="pi pi-calendar" />
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
    <div v-else class="transactions-mobile">
      <div v-if="transactions.length === 0" class="empty-state-mobile">
        <i class="pi pi-sync" />
        <h3>Aucune transaction régulière</h3>
        <p>Commencez par créer votre première transaction régulière</p>
        <Button
          class="btn-primary"
          icon="pi pi-plus"
          label="Créer"
          @click="openCreationRegularTransactionDialog"
        />
      </div>

      <div v-else class="transaction-cards">
        <div
          v-for="transaction in transactions"
          :key="transaction.id"
          class="transaction-card"
          :class="{ 'selected-card': isSelected(transaction) }"
          @click="handleRowDoubleClick({ data: transaction })"
        >
          <!-- Header de la carte -->
          <div class="card-header">
            <div class="card-header-left">
              <Tag
                :value="transaction.isIncome ? 'Recette' : 'Dépense'"
                :severity="transaction.isIncome ? 'success' : 'danger'"
                :icon="transaction.isIncome ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"
              />
            </div>
          </div>

          <!-- Corps de la carte -->
          <div class="card-body">
            <div class="card-label">
              <span class="label-text">{{ transaction.label }}</span>
            </div>

            <div class="card-info-grid">
              <div class="info-item">
                <i class="pi pi-euro" />
                <span class="card-amount" :class="transaction.isIncome ? 'income' : 'expense'">
                  {{ transaction.isIncome ? '+' : '-' }}{{ Math.abs(transaction.value).toFixed(2) }} €
                </span>
              </div>

              <div class="info-item">
                <i class="pi pi-clock" />
                <span>{{ frequencyToString(transaction.regularity) }}</span>
              </div>

              <div class="info-item">
                <i class="pi pi-calendar" />
                <span>{{ transaction.startDate }}</span>
              </div>
            </div>

            <div class="card-tag-section">
              <Tag
                :value="transaction.tagDTO.label"
                :style="getTagStyle(transaction.tagDTO.colorDTO)"
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
    />
  </div>
</template>

<style scoped lang="scss">
.regular-transactions-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 20px;
  gap: 20px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);

  @media (max-width: 768px) {
    height: auto;
    min-height: 100vh;
    padding: 10px;
    gap: 15px;
    padding-bottom: 30px;
  }
}

// ===== HEADER =====
.page-header {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(130, 42, 204, 0.08);
  border: 1px solid rgba(130, 42, 204, 0.1);

  @media (max-width: 768px) {
    padding: 16px;
    border-radius: 16px;
  }
}

.header-top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  flex: 1;

  h1 {
    font-size: 1.75rem;
    font-weight: 800;
    background: linear-gradient(135deg, #822acc, #651e9e);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 8px 0;

    @media (max-width: 768px) {
      font-size: 1.25rem;
      margin-bottom: 6px;
    }
  }
}

.page-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  @media (max-width: 768px) {
    gap: 8px;
  }
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #822acc10, #651e9e10);
  border: 1px solid rgba(130, 42, 204, 0.2);
  border-radius: 50px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #822acc;

  @media (max-width: 768px) {
    padding: 4px 10px;
    font-size: 0.75rem;
  }

  i {
    font-size: 0.75rem;

    @media (max-width: 768px) {
      font-size: 0.7rem;
    }
  }
}

// ===== ACTIONS =====
.main-actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;

  @media (max-width: 768px) {
    button {
      flex: 1;
      font-size: 0.875rem;
      padding: 0.625rem 1rem;
    }
  }
}

.btn-primary {
  background: linear-gradient(135deg, #822acc, #651e9e);
  border: none;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.25);
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, #6a22aa, #541a82);
    box-shadow: 0 6px 16px rgba(130, 42, 204, 0.35);
    transform: translateY(-2px);
  }
}

// ===== TABLE (DESKTOP) =====
.transactions-table {
  flex: 1;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(130, 42, 204, 0.08);
  border: 1px solid rgba(130, 42, 204, 0.1);

  :deep(.p-datatable) {
    .p-datatable-thead > tr > th {
      background: var(--primary);
      color: white;
      font-weight: 700;
      padding: 16px;
      border: none;
      text-transform: uppercase;
      font-size: 0.875rem;
      letter-spacing: 0.05em;
    }

    .p-datatable-tbody > tr {
      transition: all 0.2s ease;
      border-bottom: 1px solid #f3f4f6;
      background: white;
      cursor: pointer;

      &:hover {
        background: linear-gradient(135deg, #faf5ff, #f9f5ff);
      }

      > td {
        padding: 16px;
        border: none;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  i {
    font-size: 4rem;
    color: #d1d5db;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 1.25rem;
    font-weight: 700;
    color: #374151;
    margin: 0 0 8px 0;
  }

  p {
    font-size: 1rem;
    color: #6b7280;
    margin: 0 0 24px 0;
  }
}

.label-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.transaction-label {
  font-weight: 600;
  color: #1f2937;
}

.amount {
  font-weight: 800;
  font-size: 1.1rem;
  font-family: 'Courier New', monospace;

  &.expense {
    color: #ef4444;

    &::before {
      content: '- ';
    }
  }

  &.income {
    color: #10b981;

    &::before {
      content: '+ ';
    }
  }
}

.frequency-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-weight: 500;

  i {
    font-size: 0.875rem;
    color: #822acc;
  }
}

.date-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-weight: 500;

  i {
    font-size: 0.875rem;
    color: #822acc;
  }
}

:deep(.p-tag) {
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.875rem;
}

// ===== MOBILE TRANSACTIONS =====
.transactions-mobile {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  @media (max-width: 768px) {
    overflow: visible;
    flex: none;
  }
}

.empty-state-mobile {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(130, 42, 204, 0.08);

  i {
    font-size: 3rem;
    color: #d1d5db;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 1.125rem;
    font-weight: 700;
    color: #374151;
    margin: 0 0 8px 0;
  }

  p {
    font-size: 0.875rem;
    color: #6b7280;
    margin: 0 0 20px 0;
  }
}

.transaction-cards {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 768px) {
    overflow-y: visible;
    flex: none;
    gap: 16px;
    padding: 0;
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(130, 42, 204, 0.2);
    border-radius: 3px;

    &:hover {
      background: rgba(130, 42, 204, 0.4);
    }
  }
}

.transaction-card {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(130, 42, 204, 0.08);
  border: 2px solid transparent;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  @media (max-width: 768px) {
    padding: 18px;
    border-radius: 18px;
    box-shadow: 0 3px 15px rgba(130, 42, 204, 0.12);
  }

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(180deg, #822acc, #651e9e);
    transition: width 0.3s ease;

    @media (max-width: 768px) {
      width: 5px;
    }
  }

  &.selected-card {
    border-color: #822acc;
    background: linear-gradient(135deg, #faf5ff, #f9f5ff);
    box-shadow: 0 4px 16px rgba(130, 42, 204, 0.2);

    &::before {
      width: 6px;
    }

    @media (max-width: 768px) {
      box-shadow: 0 5px 20px rgba(130, 42, 204, 0.25);
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;

  @media (max-width: 768px) {
    margin-bottom: 14px;
    padding-bottom: 14px;
  }
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 768px) {
    gap: 14px;
  }
}

.card-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.label-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1f2937;
  flex: 1;
  word-break: break-word;
  line-height: 1.4;

  @media (max-width: 768px) {
    font-size: 1.15rem;
  }
}

.card-info-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;

  @media (max-width: 768px) {
    gap: 12px;
  }
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.95rem;
  color: #6b7280;

  @media (max-width: 768px) {
    font-size: 1rem;
    gap: 12px;
  }

  i {
    font-size: 1rem;
    color: #822acc;
    width: 20px;
    text-align: center;

    @media (max-width: 768px) {
      font-size: 1.1rem;
      width: 22px;
    }
  }
}

.card-amount {
  font-size: 1.4rem;
  font-weight: 800;
  font-family: 'Courier New', monospace;

  @media (max-width: 768px) {
    font-size: 1.6rem;
  }

  &.expense {
    color: #ef4444;
  }

  &.income {
    color: #10b981;
  }
}

.card-tag-section {
  display: flex;
  justify-content: flex-start;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;

  @media (max-width: 768px) {
    padding-top: 10px;
  }

  :deep(.p-tag) {
    font-size: 0.8rem;
    padding: 6px 12px;

    @media (max-width: 768px) {
      font-size: 0.85rem;
      padding: 7px 14px;
    }
  }
}
</style>
