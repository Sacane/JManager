<script setup lang="ts">
import AccountBookingDialog from '~/components/dialog/AccountBookingDialog.vue'
import useBooklet from '~/composables/useBooklet'

definePageMeta({
  layout: 'sidebar-layout',
})

const { user, isAuthenticated } = useAuth()
const toast = useJToast()
onMounted(() => {
  const currentDate = new Date()
  if (user.value == null || user.value.refreshExpirationDate > currentDate) {
    isAuthenticated.value = false
    navigateTo('/login')
  }
})

const isAccountDialogOpen = ref(false)
const { createAccount } = useBooklet()
function handleAccountCreation(account) {
  createAccount(account.label, Number(`${account.integerpart}.${account.decimalpart}`), '€')
    .then((acc) => {
      toast.success(`La création du compte ${acc.label} a été un succès !`)
      navigateTo(`/account/${acc.id}`)
    }).catch(err => toast.errorAxios(err)).finally(() => isAccountDialogOpen.value = false)
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
    <h1 class="text-3xl md:text-4xl font-bold text-center mb-8 max-w-full md:max-w-[40%]">
      Un sommaire rapide et pratique pour gérer votre budget et vos dépenses
    </h1>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-10 w-full">
      <TitleCard
        class="create-container"
        title="Créer vos livrets" description="JManager vous permet de gérer de manière indépendante vos dépenses en créant un ou plusieurs comptes.
        Vous pouvez ainsi gérer les dépenses de plusieurs personnes, entités, projets, et autres avec un seul profil."
        :on-click="createAccountIfAuthenticated"
      />
      <TitleCard
        title="Créer vos transactions"
        description="Une transaction permet de mettre à jour la vue du budget d'un compte.
        Elle contient la date à laquelle la dépense a été effectuée, le montant, le compte concerné, et son label."
      />
      <TitleCard
        title="Créer vos transactions prévisionnelles"
        description="Une transaction prévisionnelle (TP) vous permettent de prévoir des potentielles transaction, afin d'avoir une visibilité plus concrète sur vos futures dépenses.
        En un seul clique passer ces TP en transactions afin de les confirmer dans vos dépenses réels !"
      />
      <TitleCard
        title="Créer vos mensualités"
        description="Une mensualité est une transaction que vous considérez appliquable tous les mois comme la dépense d'un loyer ou bien le gain d'un salaire.
        Elles vous permettront chaque mois avec votre confirmation de pouvoir les transformer en transaction"
      />
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
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.container h1 {
  font-family: 'aktiv', sans-serif;
  font-weight: 800;
  color: var(--primary);
}
.create-container {
  &:hover {
    cursor: pointer;
  }
}
</style>
