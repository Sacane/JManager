<template>
  <div class="login-container">
    <h1 class="title">JManager Application</h1>
    <div class="form-container">
      <h2 class="form-title">{{ mode.login ? 'Se connecter' : "S'enregistrer" }}</h2>
      <div v-if="mode.login">
        <form class="form">
        <div class="form-group">
          <label for="username">Nom d'utilisateur</label>
          <input id="username" v-model="userAuth.username" type="text" class="input" />
        </div>
        <div class="form-group">
          <label for="password">Mot de passe</label>
          <input v-model="userAuth.password" type="password" class="input" />
        </div>
        <button type="button" class="btn-primary" @click="login(userAuth)">Login</button>
      </form>
      </div>
      <div v-else>
        <form class="form">
          <div class="form-group">
            <label for="username">Nom d'utilisateur *</label>
            <input id="username" v-model="userRegistered.username" type="text" class="input" />
          </div>
          <div class="form-group">
            <label for="email">Email</label>
            <input id="email" v-model="userRegistered.email" type="email" class="input" />
          </div>
          <div class="form-group">
            <label for="password">Mot de passe *</label>
            <input v-model="userRegistered.password" type="password" class="input" />
          </div>
          <div class="form-group">
            <label for="confirm">Confirmer le mot de passe *</label>
            <input id="confirm" v-model="userRegistered.confirmPassword" type="password" class="input" />
          </div>
          <button type="button" class="btn-primary" @click="register(userRegistered)">Login</button>
        </form>
      </div>
      <div class="switch-mode">
        <p>{{ mode.login ? "Vous n'avez pas de compte ?" : 'Vous avez déjà un compte ?' }}</p>
        <button @click="switchMode">{{ mode.login ? 'S\'enregistrer' : 'Se connecter' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import useAuth from '../composables/useAuth';

const { login, register } = useAuth();
const userAuth = reactive({
  username: '',
  password: '',
});

const userRegistered = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const mode = reactive({ login: true });

const switchMode = () => {
  mode.login = !mode.login;
};
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f0f0f0;
  height: 100%;
  width: 100%;
}

.title {
  font-size: 2rem;
  font-weight: bold;
  color: #8a2be2;
  margin-bottom: 1rem;
}

.form-container {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 30%;

  text-align: center;
}

.form-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #8a2be2;
  margin-bottom: 1rem;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  text-align: left;
}

label {
  font-size: 0.875rem;
  font-weight: bold;
  color: #333;
}

.input {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 0.25rem;
  font-size: 1rem;
}

.btn-primary {
  background-color: #8a2be2;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary:hover {
  background-color: #5c0e8a;
}

.switch-mode {
  margin-top: 1rem;
}

.switch-mode p {
  font-size: 0.875rem;
  color: #333;
}

.switch-mode button {
  background: none;
  border: none;
  font-size: 0.875rem;
  font-weight: bold;
  color: #8a2be2;
  cursor: pointer;
}

.switch-mode button:hover {
  text-decoration: underline;
}
</style>