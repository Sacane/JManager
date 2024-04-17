<script setup lang="ts">
definePageMeta({
  layout: 'sidebar-layout',
})

const { user, isAuthenticated } = useAuth()

onMounted(() => {
  const currentDate = new Date()
  if (user.value == null || user.value.refreshExpirationDate > currentDate) {
    isAuthenticated.value = false
    navigateTo('/login')
  }
})

function navigateIfAuthenticated() {
  if (isAuthenticated) {
    navigateTo('/account')
  }
}
</script>

<template>
  <div class="container mx-auto px-4 mt-10">
    <h1 class="text-3xl font-bold text-center mb-8">
      Un sommaire rapide et pratique pour gérer votre budget et vos dépenses
    </h1>
    <div class="card rounded-lg shadow-lg bg-white p-6 mb-8 text-center" @click="navigateIfAuthenticated()">
      <h2 class="text-2xl italic mb-4">
        Ajouter un compte
      </h2>
      <p class="text-center">
        JManager vous permet de gérer de manière indépendante vos dépenses en créant un ou plusieurs comptes.<br>
        Vous pouvez ainsi gérer les dépenses de plusieurs personnes, entités, projets, et autres avec un seul profil.
      </p>
    </div>
    <div class="card rounded-lg shadow-lg bg-white p-6 text-center" @click="navigateIfAuthenticated()">
      <h2 class="text-2xl italic mb-4">
        Ajouter une transaction
      </h2>
      <p class="text-center">
        Une transaction permet de mettre à jour la vue du budget d'un compte.<br>
        Elle contient la date à laquelle la dépense a été effectuée, le montant, le compte concerné, et son label.
      </p>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 800px;
}

.card {
  transition: transform 0.3s ease;
}

.card:hover {
  transform: translateY(-5px);
}
</style>
