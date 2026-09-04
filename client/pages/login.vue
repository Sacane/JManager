<script setup lang="ts">
import { FEATURE_KEYS } from '~/constants/featureKeys'

const { login, register } = useAuth()
const toastr = useJToast()

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')

// --- Login state ---
const userAuth = reactive({ email: '', password: '' })
const hasFailedLogin = ref(false)
const isLogging = ref(false)

// --- Register state ---
const userRegistered = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  tosAccepted: false,
  privacyAccepted: false,
})
const hasFailedRegister = ref(false)
const registerError = ref('')
const isRegistering = ref(false)

const canRegister = computed(() =>
  userRegistered.username.length > 0
  && userRegistered.email.length > 0
  && userRegistered.password.length > 0
  && userRegistered.confirmPassword.length > 0
  && userRegistered.tosAccepted
  && userRegistered.privacyAccepted,
)

const subtitle = computed(() =>
  mode.value === 'login' ? 'Connectez-vous à votre espace' : 'Créez votre compte',
)

function switchMode(target: Mode) {
  // Carry the email over so the user does not retype it after realising they are on the wrong
  // form. Only the email: copying a password across forms would surprise the user and confuse
  // credential managers (UX-48).
  if (target === 'register' && userAuth.email) {
    userRegistered.email = userAuth.email
  }
  if (target === 'login' && userRegistered.email) {
    userAuth.email = userRegistered.email
  }

  mode.value = target
  hasFailedLogin.value = false
  hasFailedRegister.value = false
  registerError.value = ''
}

async function log() {
  if (isLogging.value) return
  isLogging.value = true
  hasFailedLogin.value = false
  await login(userAuth, (e) => {
    hasFailedLogin.value = true
    toastr.errorAxios(e)
    isLogging.value = false
  })
}

async function registerUser() {
  if (isRegistering.value) return
  if (userRegistered.password !== userRegistered.confirmPassword) {
    hasFailedRegister.value = true
    registerError.value = 'Les mots de passe ne correspondent pas'
    return
  }
  isRegistering.value = true
  hasFailedRegister.value = false
  await register(
    {
      username: userRegistered.username,
      email: userRegistered.email,
      password: userRegistered.password,
      confirmPassword: userRegistered.confirmPassword,
      tosAccepted: userRegistered.tosAccepted,
      tosVersion: '1.0',
      privacyAccepted: userRegistered.privacyAccepted,
    },
    () => {
      toastr.success('Inscription réussie ! Vous pouvez maintenant vous connecter.')
      switchMode('login')
      isRegistering.value = false
    },
    (e) => {
      hasFailedRegister.value = true
      registerError.value = 'Une erreur est survenue lors de l\'inscription'
      toastr.errorAxios(e)
      isRegistering.value = false
    },
  )
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
            {{ subtitle }}
          </p>
        </div>

        <!-- Login form -->
        <form v-if="mode === 'login'" class="login-form" @submit.prevent="log">
          <div class="form-group">
            <label for="email" class="form-label">
              <i class="pi pi-envelope mr-2" />
              Adresse e-mail
            </label>
            <InputText
              id="email"
              v-model="userAuth.email"
              type="email"
              class="w-full"
              placeholder="Entrez votre adresse e-mail"
              maxlength="255"
              autocomplete="email"
            />
          </div>

          <div class="form-group">
            <label for="password" class="form-label">
              <i class="pi pi-lock mr-2" />
              Mot de passe
            </label>
            <PasswordField
              id="password"
              v-model="userAuth.password"
              placeholder="Entrez votre mot de passe"
              :maxlength="100"
              autocomplete="current-password"
            />
          </div>

          <div v-if="hasFailedLogin" class="error-message" role="alert">
            <i class="pi pi-exclamation-circle mr-2" />
            Identifiants incorrects
          </div>

          <Button
            type="submit"
            class="w-full submit-btn"
            size="large"
            :loading="isLogging"
            :disabled="isLogging || !userAuth.email || !userAuth.password"
          >
            <i class="pi pi-sign-in mr-2" />
            Se connecter
          </Button>

          <FeatureGate :feature="FEATURE_KEYS.USER_REGISTRATION">
            <div class="mode-toggle">
              <span class="mode-toggle-text">Vous n'avez pas de compte ?</span>
              <button
                type="button"
                class="toggle-link"
                data-testid="switch-to-register"
                @click="switchMode('register')"
              >
                S'inscrire
              </button>
            </div>
          </FeatureGate>
        </form>

        <!-- Registration form -->
        <form v-else class="login-form" @submit.prevent="registerUser">
          <div class="form-group">
            <label for="reg-username" class="form-label">
              <i class="pi pi-user mr-2" />
              Nom d'utilisateur
            </label>
            <InputText
              id="reg-username"
              v-model="userRegistered.username"
              type="text"
              class="w-full"
              placeholder="Choisissez un nom d'utilisateur"
              maxlength="100"
              autocomplete="username"
            />
          </div>

          <div class="form-group">
            <label for="reg-email" class="form-label">
              <i class="pi pi-envelope mr-2" />
              Adresse e-mail
            </label>
            <InputText
              id="reg-email"
              v-model="userRegistered.email"
              type="email"
              class="w-full"
              placeholder="votre@email.com"
              maxlength="255"
              autocomplete="email"
            />
          </div>

          <div class="form-group">
            <label for="reg-password" class="form-label">
              <i class="pi pi-lock mr-2" />
              Mot de passe
            </label>
            <PasswordField
              id="reg-password"
              v-model="userRegistered.password"
              placeholder="Choisissez un mot de passe"
              :maxlength="100"
              autocomplete="new-password"
            />
          </div>

          <div class="form-group">
            <label for="reg-confirm-password" class="form-label">
              <i class="pi pi-lock mr-2" />
              Confirmer le mot de passe
            </label>
            <PasswordField
              id="reg-confirm-password"
              v-model="userRegistered.confirmPassword"
              placeholder="Répétez votre mot de passe"
              :maxlength="100"
              autocomplete="new-password"
            />
          </div>

          <div class="form-group consent-group">
            <div class="consent-item">
              <Checkbox
                v-model="userRegistered.tosAccepted"
                input-id="tos-accepted"
                :binary="true"
              />
              <label for="tos-accepted" class="consent-label">
                J'accepte les
                <NuxtLink to="/terms" class="consent-link" target="_blank">Conditions Générales d'Utilisation</NuxtLink>
              </label>
            </div>
            <div class="consent-item">
              <Checkbox
                v-model="userRegistered.privacyAccepted"
                input-id="privacy-accepted"
                :binary="true"
              />
              <label for="privacy-accepted" class="consent-label">
                J'accepte la
                <NuxtLink to="/privacy" class="consent-link" target="_blank">Politique de Confidentialité</NuxtLink>
              </label>
            </div>
          </div>

          <div v-if="hasFailedRegister" class="error-message" role="alert">
            <i class="pi pi-exclamation-circle mr-2" />
            {{ registerError }}
          </div>

          <Button
            type="submit"
            class="w-full submit-btn"
            size="large"
            :loading="isRegistering"
            :disabled="isRegistering || !canRegister"
          >
            <i class="pi pi-user-plus mr-2" />
            Créer mon compte
          </Button>

          <div class="mode-toggle">
            <span class="mode-toggle-text">Vous avez déjà un compte ?</span>
            <button
              type="button"
              class="toggle-link"
              data-testid="switch-to-login"
              @click="switchMode('login')"
            >
              Se connecter
            </button>
          </div>
        </form>

        <!-- The terms were only linked from the registration checkboxes, hidden while the sign in
             form is displayed, and the sidebar carrying them only renders for authenticated
             users — so an anonymous visitor had no way to read them (UX-49). -->
        <nav class="legal-links" aria-label="Documents légaux">
          <NuxtLink to="/terms" class="legal-link">
            Conditions Générales d'Utilisation
          </NuxtLink>
          <span class="legal-sep" aria-hidden="true">·</span>
          <NuxtLink to="/privacy" class="legal-link">
            Politique de Confidentialité
          </NuxtLink>
        </nav>
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
  max-height: 100vh;
  overflow-y: auto;
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
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
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

/* Consent checkboxes */
.consent-group {
  gap: 0.75rem;
}

.consent-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
}

.consent-label {
  font-size: 0.875rem;
  color: #475569;
  line-height: 1.5;
  cursor: pointer;
}

.consent-link {
  color: #667eea;
  text-decoration: underline;
  font-weight: 500;
}

.consent-link:hover {
  color: #764ba2;
}

/* Error message */
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

/* Submit button */
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

/* Mode toggle */
.mode-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.mode-toggle-text {
  font-size: 0.875rem;
  color: #64748b;
}

.legal-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--border-color);
}

.legal-link {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.legal-link:hover {
  color: var(--primary);
  text-decoration: underline;
}

.legal-sep {
  font-size: 0.75rem;
  color: var(--text-muted);
  user-select: none;
}

.toggle-link {
  font-size: 0.875rem;
  font-weight: 600;
  color: #667eea;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  transition: color 0.2s ease;
}

.toggle-link:hover {
  color: #764ba2;
}

/* Responsive */
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
}
</style>
