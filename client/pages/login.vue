<script setup lang="ts">
import useAuth from '../composables/useAuth'

const { login, register } = useAuth()
const toastr = useJToast()
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

function switchMode() {
  mode.value = !mode.value
}

const hasFailedlogin = ref(false)
const hasFailedRegister = ref(false)

function log() {
  console.log('LOGIN')
  login(userAuth, (e) => {
    console.warn('ERROR ???')
    hasFailedlogin.value = true
    toastr.errorAxios(e)
  })
}
function registerUser() {
  register(userRegistered, () => switchMode(), (error) => {
    hasFailedRegister.value = true
    toastr.errorAxios(error, 'Erreur d\'enregistrement')
  })
}
</script>

<template>
  <div class="flex flex-col items-center justify-center w-screen h-full container">
    <img alt="keyboard background" src="@/assets/background/keyboard.jpg">
    <div class="p-8 shadow-md w-full max-w-md bg-white login-container">
      <h1 class="text-3xl text-black-600">
        JManager Application
      </h1>
      <div v-if="mode">
        <form @submit.prevent="log">
          <div class="mb-4">
            <label for="username">Nom d'utilisateur</label>
            <InputText id="username" v-model="userAuth.username" type="text" class="mt-1 p-2 w-full" />
          </div>
          <div class="mb-4">
            <label for="password">Mot de passe</label>
            <InputText v-model="userAuth.password" type="password" class="mt-1 p-2 w-full border rounded-md" />
          </div>
          <Button type="submit" class="w-full">
            Se connecter
          </Button>
          <div v-if="hasFailedlogin" class="text-red-500 mt-2">
            <h2>Le nom d'utilisateur et le mot de passe ne correspondent pas</h2>
          </div>
        </form>
      </div>

      <!--      <div v-else> -->
      <!--        <form @submit.prevent="registerUser"> -->
      <!--          <div class="mb-4"> -->
      <!--            <label for="username" class="">Nom d'utilisateur *</label> -->
      <!--            <InputText id="username" v-model="userRegistered.username" type="text" class="mt-1 p-2 w-full border rounded-md" /> -->
      <!--          </div> -->
      <!--          <div class="mb-4"> -->
      <!--            <label for="password">Mot de passe *</label> -->
      <!--            <InputText v-model="userRegistered.password" type="password" class="mt-1 p-2 w-full border rounded-md" /> -->
      <!--          </div> -->
      <!--          <div class="mb-4"> -->
      <!--            <label for="confirm">Confirmer le mot de passe *</label> -->
      <!--            <InputText id="confirm" v-model="userRegistered.confirmPassword" type="password" class="mt-1 p-2 w-full border rounded-md" /> -->
      <!--          </div> -->
      <!--          <Button type="submit" class=""> -->
      <!--            S'enregistrer -->
      <!--          </Button> -->
      <!--        </form> -->
      <!--      </div> -->

      <div class="mt-4">
        <p class="text-black-600">
          {{ mode ? "Vous n'avez pas de compte ?" : 'Vous avez déjà un compte ?' }}
        </p>
        <Button class="" @click="switchMode">
          {{ mode ? 'S\'enregistrer' : 'Se connecter' }}
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container{
  img {
    position: absolute;
    z-index: -1;
    width: 70%;
    height: 90%;
    object-fit: cover;
    border-radius: 10%;
  }
  .login-container{
    border: 1px solid #ccc;
    box-shadow: 5px 5px 15px black;
    border-radius: 30px;
  }
}
</style>
