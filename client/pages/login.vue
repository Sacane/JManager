<script setup lang="ts">
import { ref } from 'vue';
import useAuth from '../composables/useAuth';

const { login } = useAuth();

const userAuth = ref({
  username: '',
  password: '',
});
</script>

<style scoped>

/* Styles personnalisés */
.input {
  @apply p-2 border border-gray-300 rounded-lg;
}

.btn-primary {
  @apply bg-purple-600 text-white py-2 px-4 rounded-md transition hover:bg-purple-700;
}

.form-container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
}
</style>

<template>
  <div class="flex-col items-center w-full h-full mt2">
    <h1 class="text-4xl font-bold text-purple-600 mb-8">JManager application</h1>
    <div class="w-full h-auto md:h-full flex flex-col md:flex-row justify-center md:justify-evenly items-center">
      <!-- Connexion -->
      <div class="flex flex-col w-full md:w-1/3 p-6 rounded-lg border border-gray-400 form-container">
        <h1 class="text-3xl font-bold text-purple-600 text-center mb-6">
          Se connecter
        </h1>
        <form class="flex flex-col gap-4">
          <label for="username" class="text-sm font-medium text-gray-700">Nom d'utilisateur</label>
          <input id="username" v-model="userAuth.username" type="text" class="input" />
          <label for="password" class="text-sm font-medium text-gray-700">Mot de passe</label>
          <input v-model="userAuth.password" type="password" class="input" />
          <button type="button" class="btn-primary" @click="login(userAuth)">Login</button>
        </form>
      </div>

      <!-- Enregistrement -->
      <div class="flex flex-col w-full md:w-1/3 p-6 rounded-lg border border-gray-400 mt-6 md:mt-0 form-container">
        <h1 class="text-3xl font-bold text-purple-600 text-center mb-6">
          S'enregistrer
        </h1>
        <form class="flex flex-col gap-4">
          <label for="name" class="text-sm font-medium text-gray-700">Nom d'utilisateur</label>
          <input id="name" type="text" class="input" />
          <label for="email" class="text-sm font-medium text-gray-700">Email</label>
          <input id="email" type="email" class="input" />
          <label for="password" class="text-sm font-medium text-gray-700">Mot de passe</label>
          <input type="password" class="input" />
          <label for="confirm" class="text-sm font-medium text-gray-700">Confirmer le mot de passe</label>
          <input id="password" type="password" class="input" />
          <button class="btn-primary">S'enregistrer</button>
        </form>
      </div>
    </div>
  </div>
</template>

