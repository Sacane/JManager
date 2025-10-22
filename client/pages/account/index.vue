<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'
import useBooklet from '../../composables/useBooklet'

definePageMeta({
  layout: 'sidebar-layout',
})

const { fetch, deleteAccount, createAccount } = useBooklet()
const isAccountFilled = ref<boolean>(false)
const data = ref<Array<{
  id: string
  labelAccount: string
  amount: string
  currency: string
}>>([])

onMounted(async () => {
  await fetch().then((accountArray) => {
    format(accountArray)
    isAccountFilled.value = accountArray.length > 0
  })
})

function format(accounts: Array<BookletDTO>) {
  data.value = accounts.map((account: BookletDTO) => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount}`,
      currency: account.currency,
    }
  })
}

const router = useRouter()

function onCardClick(accountId: string) {
  router.push(`/account/${accountId}`)
}

function applyDelete(accountId: string) {
  deleteAccount(accountId).finally(() => {
    fetch().then((accountArray) => {
      format(accountArray)
      isAccountFilled.value = accountArray.length > 0
    })
  })
}

const isAddAccountDialogOpen = ref<boolean>(false)

function handleAccountCreation(account) {
  createAccount(account.label, account.digit, '€')
    .then(() => {
      fetch().then((accountArray) => {
        format(accountArray)
        isAccountFilled.value = accountArray.length > 0
      }).finally(() => {
        isAddAccountDialogOpen.value = false
      })
    })
}

function cancel() {
  isAddAccountDialogOpen.value = false
}

function openAccountDialog() {
  isAddAccountDialogOpen.value = true
}

function amountClass(amount: string) {
  return Number.parseFloat(amount) >= 0 ? 'positive' : 'negative'
}

function formatAmount(amount: string) {
  const num = Number.parseFloat(amount)
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num)
}
</script>

<template>
  <div class="booklets-page">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          Mes livrets
        </h1>
        <p class="page-subtitle">
          Gérez vos livrets d'épargne en toute simplicité
        </p>
      </div>
      <Button
        label="Nouveau livret"
        icon="pi pi-plus"
        class="add-button-header"
        @click="openAccountDialog"
      />
    </div>

    <div v-if="!isAccountFilled" class="empty-state">
      <i class="pi pi-wallet empty-icon" />
      <h3 class="empty-title">
        Aucun livret pour le moment
      </h3>
      <p class="empty-description">
        Créez votre premier livret pour commencer à suivre votre épargne
      </p>
      <Button
        label="Créer mon premier livret"
        icon="pi pi-plus"
        size="large"
        class="empty-action-button"
        @click="openAccountDialog"
      />
    </div>

    <div v-else class="booklets-grid">
      <div
        v-for="account in data"
        :key="account.id"
        class="booklet-card"
        @click="onCardClick(account.id)"
      >
        <div class="card-background" />
        <div class="card-content">
          <div class="card-header">
            <div class="card-icon">
              <i class="pi pi-wallet" />
            </div>
            <Button
              icon="pi pi-trash"
              class="delete-button"
              text
              rounded
              severity="danger"
              size="small"
              @click.stop="applyDelete(account.id)"
            />
          </div>

          <div class="card-body">
            <h3 class="account-label">
              {{ account.labelAccount }}
            </h3>
            <div class="amount-container">
              <span class="amount" :class="[amountClass(account.amount)]">
                {{ formatAmount(account.amount) }}
              </span>
              <span class="currency">{{ account.currency }}</span>
            </div>
          </div>

          <div class="card-footer">
            <span class="view-details">
              Voir les détails
              <i class="pi pi-arrow-right" />
            </span>
          </div>
        </div>
      </div>
    </div>

    <BookletBookingDialog
      :visible="isAddAccountDialogOpen"
      @create-account="handleAccountCreation"
      @cancel="cancel"
    />
  </div>
</template>

<style scoped lang="scss">
.booklets-page {
  min-height: 100%;
  padding: 2rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);

  @media (max-width: 768px) {
    padding: 1rem;
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
  flex-wrap: wrap;
  gap: 1.5rem;

  @media (max-width: 768px) {
    margin-bottom: 2rem;
  }
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 900;
  color: var(--grey-2);
  margin: 0 0 0.5rem 0;
  background: linear-gradient(135deg, var(--primary) 0%, var(--pink) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;

  @media (max-width: 768px) {
    font-size: 2rem;
  }
}

.page-subtitle {
  font-size: 1.1rem;
  color: #64748b;
  margin: 0;

  @media (max-width: 768px) {
    font-size: 0.95rem;
  }
}

.add-button-header {
  background: linear-gradient(135deg, var(--primary) 0%, var(--pink) 100%);
  border: none;
  padding: 0.75rem 1.5rem;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
  }

  @media (max-width: 768px) {
    width: 100%;
  }
}

// Empty State
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  text-align: center;
  max-width: 500px;
  margin: 0 auto;
}

.empty-icon {
  font-size: 4rem;
  color: var(--primary);
  opacity: 0.3;
  margin-bottom: 1.5rem;
}

.empty-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--grey-2);
  margin: 0 0 0.75rem 0;
}

.empty-description {
  font-size: 1rem;
  color: #64748b;
  margin: 0 0 2rem 0;
}

.empty-action-button {
  background: linear-gradient(135deg, var(--primary) 0%, var(--pink) 100%);
  border: none;
  padding: 0.875rem 2rem;
  font-weight: 600;
  font-size: 1.05rem;
}

// Booklets Grid
.booklets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 2rem;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }
}

// Booklet Card
.booklet-card {
  position: relative;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  min-height: 240px;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);

    .card-background {
      transform: scale(1.1);
    }

    .view-details {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.card-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--pink) 100%);
  transition: transform 0.4s ease;
}

.card-content {
  position: relative;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 3rem;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
}

.delete-button {
  color: white !important;
  opacity: 0.8;
  transition: all 0.3s ease;

  &:hover {
    opacity: 1;
    background: rgba(255, 255, 255, 0.2) !important;
  }
}

.card-body {
  flex: 1;
}

.account-label {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--grey-2);
  margin: 0 0 1rem 0;
  word-break: break-word;
}

.amount-container {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.amount {
  font-size: 2rem;
  font-weight: 900;
  line-height: 1;

  @media (max-width: 768px) {
    font-size: 1.75rem;
  }
}

.currency {
  font-size: 1.25rem;
  font-weight: 600;
  color: #64748b;
}

.positive {
  color: #10b981;
}

.negative {
  color: var(--pink);
}

.card-footer {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
}

.view-details {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--primary);
  opacity: 0.7;
  transform: translateX(-5px);
  transition: all 0.3s ease;

  i {
    font-size: 0.75rem;
  }
}

// Add Card
.add-card {
  border: 3px dashed #cbd5e1;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;

  &:hover {
    border-color: var(--primary);
    background: linear-gradient(135deg, #fef3f8 0%, #f0f9ff 100%);

    .add-icon-wrapper {
      background: linear-gradient(135deg, var(--primary) 0%, var(--pink) 100%);
      transform: rotate(90deg) scale(1.1);

      i {
        color: white;
      }
    }

    .add-text {
      color: var(--primary);
    }
  }
}

.add-card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.add-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  i {
    font-size: 1.75rem;
    color: var(--primary);
    transition: color 0.3s ease;
  }
}

.add-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: #64748b;
  transition: color 0.3s ease;
}
</style>
