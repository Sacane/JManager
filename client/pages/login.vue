<script setup lang="ts">
import type { UserAuth } from '../composables/useAuth'
definePageMeta({
  layout: 'foo',
})
const { login } = useAuth()
const userAuth = ref<UserAuth>({
  username: '',
  password: '',
})
function onSubmitLogin() {
  login(userAuth.value)
}
</script>

<template>
  <div class="w-full h-auto md:h-full flex flex-col md:flex-row justify-center md:justify-evenly items-center">
    <div class="flex flex-col w-full md:w-30% p-10 rounded-lg border border-gray-400">
      <h1 class="text-3xl font-bold text-center">
        Se connecter
      </h1>
      <div class="flex flex-col gap-2 p-3 border-rd-20px border-none">
        <label for="username">Nom d'utilisateur</label>
        <input id="username" v-model="userAuth.username" type="text" class="border border-gray-400 rounded-lg p-1 ">
        <label for="password">Password</label>
        <input v-model="userAuth.password" type="password" class="border border-gray-400 rounded-lg mb-4 p-1">
        <button type="submit" class="bg-#7F52FF text-white p-7px rounded-2" @click="onSubmitLogin">
          Login
        </button>
      </div>
    </div>
    <div class="flex flex-col w-full md:w30% p-10 rounded-lg border border-gray-400">
      <h1 class="text-3xl font-bold text-center">
        S'enregistrer
      </h1>
      <form class="flex flex-col gap-2 p-10 border-rd-20px border-none">
        <label for="name">Nom d'utilisateur</label>
        <input id="name" type="text" class="border border-gray-400 rounded-lg p-0.5">
        <label for="email">Email</label>
        <input id="email" type="email" class="border border-gray-400 rounded-lg p-0.5">
        <label for="password">Mot de passe</label>
        <input type="password" class="border border-gray-400 rounded-lg p-0.5">
        <label for="confirm">Confirmer le mot de passe</label>
        <input id="password" type="password" class="border border-gray-400 rounded-lg mb-4 p-0.5">
        <button class="bg-#7F52FF text-white p-7px rounded-2">
          S'enregistrer
        </button>
      </form>
    </div>
  </div>
</template>
