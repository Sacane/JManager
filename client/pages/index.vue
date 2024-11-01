<script setup lang="ts">
import AccountBookingDialog from '~/components/dialog/AccountBookingDialog.vue'
import useAccounts from '~/composables/useAccounts'

definePageMeta({
  layout: 'sidebar-layout',
})

const { user, isAuthenticated } = useAuth()
const toastr = useJToast()
onMounted(() => {
  const currentDate = new Date()
  if (user.value == null || user.value.refreshExpirationDate > currentDate) {
    isAuthenticated.value = false
    navigateTo('/login')
  }
})

const isAccountDialogOpen = ref(false)
const { createAccount } = useAccounts()
function handleAccountCreation(account) {
  createAccount(account.label, `${account.integerpart}.${account.decimalpart} €`)
    .then((acc) => {
      toastr.success(`La création du compte ${acc.label} a été un succès !`)
      navigateTo(`/account/${acc.id}`)
    }).catch(err => toastr.errorAxios(err)).finally(() => isAccountDialogOpen.value = false)
}
function cancel() {
  isAccountDialogOpen.value = false
}
function createAccountIfAuthenticated() {
  if (isAuthenticated) {
    isAccountDialogOpen.value = true
  }
}
</script>

<template>
  <div class="container mx-auto px-4 mt-10">
    <h1 class="text-3xl font-bold text-center mb-8 ">
      Un sommaire rapide et pratique pour gérer votre budget et vos dépenses
    </h1>
    <div class="card rounded-lg shadow-lg bg-white p-6 mb-8 text-center create-container" @click="createAccountIfAuthenticated()">
      <h2 class="text-2xl italic mb-4">
        Ajouter un compte
      </h2>
      <p class="text-center">
        JManager vous permet de gérer de manière indépendante vos dépenses en créant un ou plusieurs comptes.<br>
        Vous pouvez ainsi gérer les dépenses de plusieurs personnes, entités, projets, et autres avec un seul profil.
      </p>
    </div>
    <div class="card rounded-lg shadow-lg bg-white p-6 text-center">
      <h2 class="text-2xl italic mb-4">
        Ajouter une transaction
      </h2>
      <p class="text-center">
        Une transaction permet de mettre à jour la vue du budget d'un compte.<br>
        Elle contient la date à laquelle la dépense a été effectuée, le montant, le compte concerné, et son label.
      </p>
    </div>
    <AccountBookingDialog
      integerpart="0"
      decimalpart="0"
      :visible="isAccountDialogOpen"
      @create-account="handleAccountCreation"
      @cancel="cancel"
    />
  </div>
</template>

<style scoped>
.container {
  max-width: 800px;
}
.container h1{
  font-family: 'aktiv', sans-serif;
  font-weight: 800;
  color: var(--primary);
}

.card {
  transition: transform 0.3s ease;
}
.create-container{
  &:hover{
    cursor: pointer;
  }
}

.card:hover {
  transform: translateY(-5px);
}
</style>
