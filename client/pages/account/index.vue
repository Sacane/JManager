<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import useBooklet from '../../composables/useBooklet'
import AccountBookingDialog from '~/components/dialog/AccountBookingDialog.vue'

definePageMeta({
  layout: 'sidebar-layout',
})

const { fetch, deleteAccount, createAccount } = useBooklet()
const isAccountFilled = ref<boolean>(false)
const data = ref<Array<AccountDTO>>([])

onMounted(async () => {
  await fetch().then((accountArray) => {
    format(accountArray)
    isAccountFilled.value = accountArray.length > 0
  })
})

function format(accounts: Array<AccountDTO>) {
  data.value = accounts.map((account: AccountDTO) => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount}`,
      currency: account.currency,
    }
  })
}

const router = useRouter()

function onCardClick(accountId: number) {
  router.push(`/account/${accountId}`)
}

function applyDelete(accountId: number) {
  deleteAccount(accountId).finally(() => {
    fetch().then((accountArray) => {
      format(accountArray)
      isAccountFilled.value = accountArray.length > 0
    })
  })
}

const isAddAccountDialogOpen = ref<boolean>(false)
const newAccount = reactive({
  label: '',
  amount: 0.00,
})

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
</script>

<template>
  <div class="w-full h-full flex flex-col gap-5 items-center">
    <div class="p20px container">
      <h2 class="info-text">
        Mes livrets
      </h2>
      <div class="account-cards">
        <div v-for="account in data" :key="account.id" class="card" @click="onCardClick(account.id)">
          <div class="card-header">
            <h3>{{ account.labelAccount }}</h3>
            <Button icon="pi pi-trash" class="delete-button" severity="danger" @click.stop="applyDelete(account.id)" />
          </div>
          <div class="card-body">
            <p>{{ account.amount }} {{ account.currency }}</p>
          </div>
        </div>
        <Button icon="pi pi-plus" class="add-button" @click="openAccountDialog" />
      </div>
    </div>
    <AccountBookingDialog
      :digit="newAccount.amount"
      :visible="isAddAccountDialogOpen"
      @create-account="handleAccountCreation"
      @cancel="cancel"
    />
  </div>
</template>

<style scoped lang="scss">
.container {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.info-text {
  text-align: center;
  color: #555;
  margin-bottom: 20px;
  font-weight: 900;
  font-size: 2rem;
}

.account-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.card {
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  width: 300px;
  cursor: pointer;
  transition: transform 0.2s;
}

.card:hover {
  transform: scale(1.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-body {
  margin-top: 10px;
}

.delete-button {
  background-color: transparent;
  border: none;
  color: red;
  cursor: pointer;
}

.add-button {
  font-size: 2rem;
  background-color: var(--primary-color);
  color: white;
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
