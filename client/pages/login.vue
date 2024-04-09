<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-100">
    <h1 class="text-3xl font-bold text-purple-600 mb-4">JManager Application</h1>
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
      <h2 class="text-2xl font-bold text-purple-600 mb-4">{{ mode ? 'Se connecter' : "S'enregistrer" }}</h2>

      <div v-if="mode">
        <form @submit.prevent="log">
          <div class="mb-4">
            <label for="username" class="block text-sm font-medium text-gray-600">Nom d'utilisateur</label>
            <input id="username" v-model="userAuth.username" type="text" class="mt-1 p-2 w-full border rounded-md" />
          </div>
          <div class="mb-4">
            <label for="password" class="block text-sm font-medium text-gray-600">Mot de passe</label>
            <input v-model="userAuth.password" type="password" class="mt-1 p-2 w-full border rounded-md" />
          </div>
          <button type="submit" class="bg-purple-600 text-white px-4 py-2 rounded-md hover:bg-purple-700 transition-colors">
            Login
          </button>
          <div v-if="hasFailedlogin" class="text-red-500 mt-2">
            <h2>Le nom d'utilisateur et le mot de passe ne correspondent pas</h2>
          </div>
        </form>
      </div>

      <div v-else>
        <form @submit.prevent="registerUser">
          <div class="mb-4">
            <label for="username" class="block text-sm font-medium text-gray-600">Nom d'utilisateur *</label>
            <input id="username" v-model="userRegistered.username" type="text" class="mt-1 p-2 w-full border rounded-md" >
          </div>
          <div class="mb-4">
            <label for="email" class="block text-sm font-medium text-gray-600">Email</label>
            <input id="email" v-model="userRegistered.email" type="email" class="mt-1 p-2 w-full border rounded-md" >
          </div>
          <div class="mb-4">
            <label for="password" class="block text-sm font-medium text-gray-600">Mot de passe *</label>
            <input v-model="userRegistered.password" type="password" class="mt-1 p-2 w-full border rounded-md" >
          </div>
          <div class="mb-4">
            <label for="confirm" class="block text-sm font-medium text-gray-600">Confirmer le mot de passe *</label>
            <input id="confirm" v-model="userRegistered.confirmPassword" type="password" class="mt-1 p-2 w-full border rounded-md">
          </div>
          <button type="submit" class="bg-purple-600 text-white px-4 py-2 rounded-md hover:bg-purple-700 transition-colors">
            S'enregistrer
          </button>
          <div v-if="hasFailedRegister" class="text-red-500 mt-2">
            <h2>Les mots de passe ne correspondent pas</h2>
          </div>
        </form>
      </div>

      <div class="mt-4">
        <p class="text-gray-600">{{ mode ? "Vous n'avez pas de compte ?" : 'Vous avez déjà un compte ?' }}</p>
        <button @click="switchMode" class="text-purple-600 font-semibold hover:underline">
          {{ mode ? 'S\'enregistrer' : 'Se connecter' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import useAuth from '../composables/useAuth'

const { login, register } = useAuth()
const userAuth = reactive({
  username: '',
  password: '',
})

const userRegistered = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const mode = ref(true)

function switchMode () {
  mode.value = !mode.value
}

const hasFailedlogin = ref(false)
const hasFailedRegister = ref(false)

function log() {
  login(userAuth, (e) => {
    hasFailedlogin.value = true
    console.error(e)
  })
}
function registerUser() {
  register(userRegistered, () => switchMode(), () => {
    hasFailedRegister.value = true
  })
}
</script>

<style scoped>
/* You can keep your scoped styles here, they don't need to be changed */
</style>
