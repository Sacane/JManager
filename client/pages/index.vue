import useAuth from '../composables/useAuth';
<script setup lang="ts">
definePageMeta({
  layout: 'sidebar-layout',
})

const {user, isAuthenticated} = useAuth()

onMounted(() => {
  const currentDate = new Date();
  console.log(user.value?.tokenExpirationDate)
  if(user.value == null || user.value.refreshExpirationDate > currentDate) {
    isAuthenticated.value = false
    navigateTo('/login')
  }
})
</script>

<template>
  <div class="flex">
    <h1 class="text-3xl font-bold text-center mt-10">
      Un sommaire rapide et pratique pour gérer votre budget et vos dépenses
    </h1>
    <section class="text-center">
      <h2 class="text-2xl mt-2xl italic">
        Ajouter un compte
      </h2>
      <p>
        JManager vous permet de gérer de manière indépendantes vos dépenses en créant un ou plusieurs comptes.<br>
        Vous pouvez ainsi gérer les dépenses de plusieurs personnes, entités, projets, et autres avec un seul profil.
      </p>
    </section>
    <section class="text-center">
      <h2 class="text-2xl mt-2xl italic">
        Ajouter une fiche
      </h2>
      <p>
        Une fiche est une ligne à renseigner pour mettre à jour l'état de son budget.<br>
        Elle contient la date à laquelle la dépense à été effectué, le montant, le compte concerné, et son label.
      </p>
    </section>
  </div>
</template>
