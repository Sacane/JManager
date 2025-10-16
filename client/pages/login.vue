<script setup lang="ts">
import useAuth from '../composables/useAuth'

const { login } = useAuth()
const toastr = useJToast()
const userAuth = reactive({
  username: '',
  password: '',
})

const hasFailedlogin = ref(false)

function log() {
  login(userAuth, (e) => {
    hasFailedlogin.value = true
    toastr.errorAxios(e)
  })
}
</script>

<template>
  <div class="login-wrapper">
    <div class="gradient-background" />

    <div class="decorative-shape shape-1" />
    <div class="decorative-shape shape-2" />
    <div class="decorative-shape shape-3" />

    <div class="login-content">
      <div class="login-card">
        <div class="card-header">
          <div class="logo-circle">
            <img src="/favicon.ico" alt="JManager Logo" class="app-logo">
          </div>
          <h1 class="app-title">
            JManager Application
          </h1>
          <p class="app-subtitle">
            Connectez-vous à votre espace
          </p>
        </div>

        <form class="login-form" @submit.prevent="log">
          <div class="form-group">
            <label for="username" class="form-label">
              <i class="pi pi-user mr-2" />
              Nom d'utilisateur
            </label>
            <InputText
              id="username"
              v-model="userAuth.username"
              type="text"
              class="w-full"
              placeholder="Entrez votre nom d'utilisateur"
            />
          </div>

          <div class="form-group">
            <label for="password" class="form-label">
              <i class="pi pi-lock mr-2" />
              Mot de passe
            </label>
            <InputText
              id="password"
              v-model="userAuth.password"
              type="password"
              class="w-full"
              placeholder="Entrez votre mot de passe"
            />
          </div>

          <div v-if="hasFailedlogin" class="error-message">
            <i class="pi pi-exclamation-circle mr-2" />
            Le nom d'utilisateur et le mot de passe ne correspondent pas
          </div>

          <Button type="submit" class="w-full submit-btn" size="large">
            <i class="pi pi-sign-in mr-2" />
            Se connecter
          </Button>
        </form>

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

        <!--      <div class="mt-4"> -->
        <!--                <p class="text-black-600"> -->
        <!--                  {{ mode ? "Vous n'avez pas de compte ?" : 'Vous avez déjà un compte ?' }} -->
        <!--                </p> -->
        <!--        <Button> -->
        <!--          Se connecter -->
        <!--        </Button> -->
        <!--      </div> -->
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gradient-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg,
    #667eea 0%,
    #764ba2 25%,
    #f093fb 50%,
    #4facfe 75%,
    #00f2fe 100%
  );
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
  z-index: 0;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* Formes décoratives */
.decorative-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  z-index: 1;
}

.shape-1 {
  width: 500px;
  height: 500px;
  background: white;
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: white;
  bottom: -100px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: white;
  top: 50%;
  right: 10%;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-30px) rotate(5deg); }
}

.login-content {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 450px;
  padding: 2rem;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 3rem 2.5rem;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;
  animation: slideIn 0.6s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.logo-circle {
  width: 100px;
  height: 100px;
  margin: 0 auto 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

.app-logo {
  width: 100px;
  height: 100px;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.app-title {
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 0.5rem;
}

.app-subtitle {
  color: #64748b;
  font-size: 0.95rem;
  font-weight: 500;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 600;
  color: #334155;
  font-size: 0.95rem;
  display: flex;
  align-items: center;
}

.form-label i {
  color: #667eea;
}

.error-message {
  display: flex;
  align-items: center;
  padding: 1rem;
  background: linear-gradient(135deg, #fee 0%, #fdd 100%);
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.9rem;
  font-weight: 500;
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  75% { transform: translateX(10px); }
}

.submit-btn {
  margin-top: 0.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-weight: 600;
  font-size: 1rem;
  padding: 0.75rem;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

@media (max-width: 640px) {
  .login-content {
    padding: 1rem;
  }

  .login-card {
    padding: 2rem 1.5rem;
  }

  .app-title {
    font-size: 1.5rem;
  }

  .logo-circle {
    width: 80px;
    height: 80px;
  }

  .logo-circle i {
    font-size: 2.5rem;
  }
}
</style>
