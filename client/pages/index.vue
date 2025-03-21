<!--
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
-->

<script setup lang="ts">
import useAuth from '@/composables/useAuth'
import AccountBookingDialog from '~/components/dialog/AccountBookingDialog.vue'

definePageMeta({
  layout: 'sidebar-layout',
})
const { user } = useAuth()
const { createAccount, fetch } = useBooklet()
const isAccountDialogOpen = ref(false)
const toast = useJToast()

const accounts = ref<AccountDTO[]>([])
const sum = computed(() => accounts.value.reduce((acc, curr) => acc + Number.parseFloat(curr.amount), 0))

function handleAccountCreation(account: Account) {
  createAccount(account.label, `${account.integerpart}.${account.decimalpart} €`)
    .then((acc) => {
      if (accounts.value.length < 3) {
        accounts.value.push(acc)
      }
      toast.success('Le compte a bien été créé')
      navigateTo(`/account/${acc.id}`)
    }).catch(err => toast.errorAxios(err))
}
function cancel() {
  isAccountDialogOpen.value = false
}
function onDialogOpen() {
  if (accounts.value.length === 0) {
    isAccountDialogOpen.value = true
  }
}

onMounted(() => {
  fetch().then((result) => {
    for (let i = 0; i < result.length; i++) {
      accounts.value.push(result[i])
    }
  })
})
</script>

<template>
  <div class="lg:self-center h-full w-full flex flex-row mt-50px justify-center">
    <div class="flex flex-col division max-w[90%] gap-5 lg:(w-full max-w[50%] gap-15px)">
      <div class="flex flex-col gap-20px lg:(line w-full flex flex-row)">
        <div class="content profile lg:( w-[25%] flex flex-col justify-center align-center )">
          <div class="user-icon-container">
            <i class="pi pi-user user-icon" />
          </div>
          <h2>
            {{ user?.username }}
          </h2>
        </div>
        <div class="content lg:(w-[75%] card)" @click="onDialogOpen">
          <div class="card-header">
            <div class="title-container">
              <h1>Livrets</h1>
            </div>
          </div>
          <div v-if="accounts.length === 0" class="card-body">
            <i class="pi pi-plus icon-large" />
            <p>Créer votre premier livret</p>
          </div>
          <div v-else class="card-body">
            <div class="w-full flex flex-row justify-evenly">
              <div>
                <p>Totalité des revenus : {{ sum }} €</p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="flex flex-col gap-20px line lg:(w-full flex flex-row)">
        <div class="content title-container lg:w-[67%]">
          <div class="card-header">
            <div class="title-container">
              <h1>Mensualités</h1>
            </div>
          </div>
          <div class="card-body">
            <i class="pi pi-plus icon-large" />
            <p>Créer votre première mensualité</p>
          </div>
        </div>
        <div class="content lg:w-[33%]">
          <div class="card-header">
            <div class="title-container">
              <h1>Tags</h1>
            </div>
          </div>
          <div class="card-body">
            <i class="pi pi-plus icon-large" />
            <p>Créer votre premier tag personnel</p>
          </div>
        </div>
      </div>
    </div>
  </div>
  <AccountBookingDialog
    integerpart="0"
    decimalpart="0"
    :visible="isAccountDialogOpen"
    @create-account="handleAccountCreation"
    @cancel="cancel"
  />
</template>

<style scoped>
.division {
  transition: ease 1s;
}
.content {
  background: linear-gradient(135deg, var(--primary), #651e9e);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-radius: 20px;
  transition: transform 0.3s ease;
  color: #fff;
  cursor: pointer;
  &:hover {
    transform: scale(1.05);
  }
}
.profile {
  display: flex;
  justify-content: center;
  align-items: center;
  @media (max-width: 780px) {
    justify-content: space-evenly;
    .user-icon-container {
      height: 110px;
    }
  }
}
.user-icon-container {
  margin: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  border-radius: 50%;
  width: 100px;
  height: 100px;
  @media (max-width: 780px) {
    height: 70px;

  }
}

.user-icon {
  color: var(--primary);
  font-size: 50px;
}

.card {
  background: #fff;
  border-radius: 12px;
  color: #333;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  font-family: Arial, sans-serif;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

/* Header de la carte */
.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
}

p {
  margin: 0;
  font-size: 20px;
}

/* Corps de la carte */
.card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.icon-large {
  font-size: 36px;
  color: var(--primary);
}

.card-body p {
  @media (max-width: 780px) {
    font-size: 18px;
  }
  font-size: 25px;
  margin: 15px;
}

h3 {
  margin: 15px;
}
</style>
