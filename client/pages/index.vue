<script setup lang="ts">
import { useIntersectionObserver } from '@vueuse/core'
import useAuth from '@/composables/useAuth'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'

definePageMeta({
  layout: 'default',
})

const { user } = useAuth()
const { createAccount, fetch } = useBooklet()
const isAccountDialogOpen = ref(false)
const toast = useJToast()

const accounts = ref<BookletDTO[]>([])
const sum = computed(() => accounts.value.reduce((acc: number, curr: BookletDTO) => acc + Number.parseFloat(curr.amount.toString()), 0.00))

const heroRef = ref(null)
const featuresRef = ref(null)
const statsRef = ref(null)
const isHeroVisible = ref(false)
const isFeaturesVisible = ref(false)
const isStatsVisible = ref(false)

useIntersectionObserver(heroRef, ([{ isIntersecting }]) => {
  if (isIntersecting) {
    isHeroVisible.value = true
  }
}, { threshold: 0.1 })

useIntersectionObserver(featuresRef, ([{ isIntersecting }]) => {
  if (isIntersecting) {
    isFeaturesVisible.value = true
  }
}, { threshold: 0.2 })

useIntersectionObserver(statsRef, ([{ isIntersecting }]) => {
  if (isIntersecting) {
    isStatsVisible.value = true
  }
}, { threshold: 0.2 })

function handleAccountCreation(account: { label: string, digit: number }) {
  createAccount(account.label, account.digit, '€')
    .then((acc) => {
      if (accounts.value.length < 3) {
        accounts.value.push(acc)
      }
      toast.success('Le compte a bien été créé')
    })
    .catch(err => toast.errorAxios(err))
}

function cancel() {
  isAccountDialogOpen.value = false
}

onMounted(() => {
  fetch().then((result) => {
    accounts.value = result
  })
})
</script>

<template>
  <div class="landing-container">
    <!-- Navigation Button -->
    <div class="dashboard-nav">
      <button class="dashboard-button" @click="navigateTo('/dashboard')">
        <i class="pi pi-home" />
        <span>Retour au Dashboard</span>
      </button>
    </div>
    <!-- Hero Section -->
    <section ref="heroRef" class="hero-section" :class="{ visible: isHeroVisible }">
      <div class="hero-content">
        <div class="hero-text">
          <div class="welcome-badge">
            <i class="pi pi-sparkles" />
            <span>Bienvenue {{ user?.username }}</span>
          </div>
          <h1 class="hero-title">
            Prenez le contrôle de vos
            <span class="highlight-text">finances</span>
          </h1>
          <p class="hero-description">
            Gérez vos dépenses quotidiennes, visualisez vos budgets et planifiez votre avenir financier en toute simplicité
          </p>
          <div class="hero-cta">
            <button v-if="accounts.length === 0" class="cta-primary" @click="isAccountDialogOpen = true">
              <i class="pi pi-plus-circle" />
              <span>Créer mon premier livret</span>
            </button>
            <div v-else class="booklets-list">
              <div class="booklets-header">
                <h3>Mes livrets</h3>
                <button class="add-booklet-btn" title="Ajouter un livret" @click="isAccountDialogOpen = true">
                  <i class="pi pi-plus" />
                </button>
              </div>
              <div class="booklets-grid">
                <div
                  v-for="account in accounts"
                  :key="account.id"
                  class="booklet-card"
                  @click="navigateTo(`/account/${account.id}`)"
                >
                  <div class="booklet-icon">
                    <i class="pi pi-book" />
                  </div>
                  <div class="booklet-info">
                    <h4 class="booklet-label">
                      {{ account.labelAccount }}
                    </h4>
                    <p class="booklet-amount">
                      {{ Number.parseFloat(account.amount.toString()).toFixed(2) }} {{ account.currency }}
                    </p>
                  </div>
                  <div class="booklet-arrow">
                    <i class="pi pi-arrow-right" />
                  </div>
                </div>
              </div>
            </div>
            <button v-if="accounts.length > 0" class="cta-secondary" @click="navigateTo('/dashboard')">
              <i class="pi pi-arrow-right" />
              <span>Accéder à mon tableau de bord</span>
            </button>
            <div class="quick-stats">
              <div class="stat-item">
                <i class="pi pi-wallet" />
                <span>{{ accounts.length }} livret{{ accounts.length > 1 ? 's' : '' }}</span>
              </div>
              <div v-if="accounts.length > 0" class="stat-item highlight">
                <i class="pi pi-chart-line" />
                <span>{{ sum.toFixed(2) }} €</span>
              </div>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="floating-card card-1">
            <div class="card-icon">
              <i class="pi pi-wallet" />
            </div>
            <div class="card-content">
              <span class="card-label">Solde total</span>
              <span class="card-value">{{ sum.toFixed(2) }} €</span>0
            </div>
          </div>
          <div class="floating-card card-2">
            <div class="card-icon success">
              <i class="pi pi-arrow-up" />
            </div>
            <div class="card-content">
              <span class="card-label">Revenus</span>
              <span class="card-value success-text">+1,245 €</span>
            </div>
          </div>
          <div class="floating-card card-3">
            <div class="card-icon warning">
              <i class="pi pi-arrow-down" />
            </div>
            <div class="card-content">
              <span class="card-label">Dépenses</span>
              <span class="card-value warning-text">-876 €</span>
            </div>
          </div>
          <div class="center-circle">
            <div class="pulse-ring" />
            <div class="pulse-ring delay-1" />
            <div class="pulse-ring delay-2" />
            <i class="pi pi-chart-pie" />
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section ref="statsRef" class="stats-section" :class="{ visible: isStatsVisible }">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="pi pi-book" />
          </div>
          <h3>{{ accounts.length }}</h3>
          <p>Livret{{ accounts.length > 1 ? 's' : '' }} actif{{ accounts.length > 1 ? 's' : '' }}</p>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <i class="pi pi-calendar" />
          </div>
          <h3>100%</h3>
          <p>Visibilité mensuelle</p>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <i class="pi pi-bolt" />
          </div>
          <h3>Instantané</h3>
          <p>Suivi en temps réel</p>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section ref="featuresRef" class="features-section" :class="{ visible: isFeaturesVisible }">
      <h2 class="section-title">
        Tout ce dont vous avez besoin pour
        <span class="highlight-text">gérer vos finances</span>
      </h2>

      <div class="features-grid">
        <div
          class="feature-card"
          @click="isAccountDialogOpen = true"
        >
          <div class="feature-icon gradient-1">
            <i class="pi pi-book" />
          </div>
          <h3>Livrets de compte</h3>
          <p>Créez plusieurs livrets pour organiser vos finances par projet ou objectif</p>
          <div class="feature-action">
            <span v-if="accounts.length === 0">Créer votre premier livret</span>
            <span v-else>{{ accounts.length }} livret{{ accounts.length > 1 ? 's' : '' }} créé{{ accounts.length > 1 ? 's' : '' }}</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>

        <div class="feature-card" @click="navigateTo('/transactions')">
          <div class="feature-icon gradient-2">
            <i class="pi pi-chart-line" />
          </div>
          <h3>Transactions intelligentes</h3>
          <p>Suivez vos dépenses et recettes avec une vue mensuelle et annuelle détaillée</p>
          <div class="feature-action">
            <span>Gérer mes transactions</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>

        <div class="feature-card" @click="navigateTo('/tags')">
          <div class="feature-icon gradient-3">
            <i class="pi pi-tags" />
          </div>
          <h3>Organisation par tags</h3>
          <p>Catégorisez vos transactions pour une meilleure analyse de vos habitudes</p>
          <div class="feature-action">
            <span>Créer des tags</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>

        <div class="feature-card highlight-card">
          <div class="feature-icon gradient-4">
            <i class="pi pi-clock" />
          </div>
          <h3>Transactions prévisionnelles</h3>
          <p>Anticipez vos dépenses avec un solde théorique distinct de votre solde réel</p>
          <div class="feature-badge">
            <i class="pi pi-star-fill" />
            <span>Fonctionnalité avancée</span>
          </div>
          <div class="feature-action">
            <span>Découvrir</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>

        <div class="feature-card">
          <div class="feature-icon gradient-5">
            <i class="pi pi-refresh" />
          </div>
          <h3>Transactions régulières</h3>
          <p>Configurez vos paiements récurrents et ne manquez plus jamais une échéance</p>
          <div class="feature-action">
            <span>Configurer</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>

        <div class="feature-card">
          <div class="feature-icon gradient-6">
            <i class="pi pi-shield" />
          </div>
          <h3>Sécurité & Confidentialité</h3>
          <p>Vos données financières sont protégées et totalement confidentielles</p>
          <div class="feature-action">
            <span>En savoir plus</span>
            <i class="pi pi-arrow-right" />
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <div class="cta-content">
        <h2>Prêt à prendre en main vos finances ?</h2>
        <p>Rejoignez les étudiants et professionnels qui font confiance à notre solution</p>
        <button class="cta-button" @click="navigateTo('/dashboard')">
          <i class="pi pi-play" />
          <span>Commencer maintenant</span>
        </button>
      </div>
      <div class="cta-decoration">
        <div class="decoration-circle circle-1" />
        <div class="decoration-circle circle-2" />
        <div class="decoration-circle circle-3" />
      </div>
    </section>
  </div>

  <BookletBookingDialog
    :digit="0.00"
    :visible="isAccountDialogOpen"
    @create-account="handleAccountCreation"
    @cancel="cancel"
  />
</template>

<style scoped>
.landing-container {
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
}

/* ===== DASHBOARD NAV ===== */
.dashboard-nav {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  animation: fadeSlideDown 0.6s ease-out;
}

.dashboard-button {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  color: #822acc;
  padding: 12px 24px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  border: 2px solid rgba(130, 42, 204, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.dashboard-button:hover {
  background: #822acc;
  color: white;
  border-color: #822acc;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(130, 42, 204, 0.3);
}

.dashboard-button i {
  font-size: 18px;
}

/* ===== HERO SECTION ===== */
.hero-section {
  position: relative;
  min-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #822acc 0%, #651e9e 50%, #4a1575 100%);
  overflow: hidden;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 80%, rgba(224, 216, 36, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.hero-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.hero-content {
  max-width: 1200px;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  position: relative;
  z-index: 1;
}

.hero-text {
  color: white;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  padding: 8px 20px;
  border-radius: 50px;
  font-size: 14px;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: fadeSlideDown 0.6s ease-out 0.2s backwards;
}

.welcome-badge i {
  color: #e0d824;
}

.hero-title {
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 24px;
  animation: fadeSlideDown 0.6s ease-out 0.3s backwards;
}

.highlight-text {
  background: linear-gradient(135deg, #e0d824 0%, #f5e84a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: inline-block;
  position: relative;
}

.hero-description {
  font-size: 1.25rem;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 40px;
  animation: fadeSlideDown 0.6s ease-out 0.4s backwards;
}

.hero-cta {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fadeSlideDown 0.6s ease-out 0.5s backwards;
}

.cta-primary {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #e0d824 0%, #f5e84a 100%);
  color: #4a1575;
  padding: 18px 36px;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 30px rgba(224, 216, 36, 0.3);
  width: fit-content;
}

.cta-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(224, 216, 36, 0.4);
}

.cta-secondary {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  color: white;
  padding: 18px 36px;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 700;
  border: 2px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
  width: fit-content;
}

.cta-secondary:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-3px);
}

.quick-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 50px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-weight: 600;
}

.stat-item.highlight {
  background: rgba(224, 216, 36, 0.2);
  border-color: rgba(224, 216, 36, 0.4);
}

/* ===== BOOKLETS LIST ===== */
.booklets-list {
  width: 100%;
  margin-bottom: 20px;
}

.booklets-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.booklets-header h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.add-booklet-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  font-size: 18px;
}

.add-booklet-btn:hover {
  background: rgba(224, 216, 36, 0.3);
  border-color: rgba(224, 216, 36, 0.6);
  transform: rotate(90deg) scale(1.1);
}

.booklets-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* Scrollbar personnalisée */
.booklets-grid::-webkit-scrollbar {
  width: 6px;
}

.booklets-grid::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}

.booklets-grid::-webkit-scrollbar-thumb {
  background: rgba(224, 216, 36, 0.5);
  border-radius: 10px;
}

.booklets-grid::-webkit-scrollbar-thumb:hover {
  background: rgba(224, 216, 36, 0.7);
}

.booklet-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.booklet-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(224, 216, 36, 0.1), rgba(224, 216, 36, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.booklet-card:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(224, 216, 36, 0.6);
  transform: translateX(8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.booklet-card:hover::before {
  opacity: 1;
}

.booklet-icon {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #e0d824, #f5e84a);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 24px;
  color: #4a1575;
  box-shadow: 0 4px 12px rgba(224, 216, 36, 0.3);
  transition: all 0.3s ease;
  z-index: 1;
}

.booklet-card:hover .booklet-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 16px rgba(224, 216, 36, 0.5);
}

.booklet-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 1;
}

.booklet-label {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  margin: 0;
  line-height: 1.2;
}

.booklet-amount {
  font-size: 1.3rem;
  font-weight: 800;
  color: #e0d824;
  margin: 0;
  line-height: 1;
}

.booklet-arrow {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: white;
  flex-shrink: 0;
  transition: all 0.3s ease;
  z-index: 1;
}

.booklet-card:hover .booklet-arrow {
  background: rgba(224, 216, 36, 0.4);
  transform: translateX(4px);
}

/* ===== HERO VISUAL ===== */
.hero-visual {
  position: relative;
  height: 500px;
  animation: fadeSlideUp 0.8s ease-out 0.4s backwards;
}

.center-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(224, 216, 36, 0.3), rgba(224, 216, 36, 0.1));
  backdrop-filter: blur(20px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.center-circle i {
  font-size: 80px;
  color: white;
  z-index: 2;
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 3px solid rgba(224, 216, 36, 0.6);
  border-radius: 50%;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.pulse-ring.delay-1 {
  animation-delay: 0.5s;
}

.pulse-ring.delay-2 {
  animation-delay: 1s;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 16px 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: float 3s ease-in-out infinite;
}

.floating-card.card-1 {
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.floating-card.card-2 {
  top: 20%;
  right: 5%;
  animation-delay: 0.5s;
}

.floating-card.card-3 {
  bottom: 15%;
  left: 5%;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

.card-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.card-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
}

.card-icon.warning {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.card-content {
  display: flex;
  flex-direction: column;
}

.card-label {
  font-size: 12px;
  color: #b1aeae;
  font-weight: 500;
}

.card-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.success-text {
  color: #10b981;
}

.warning-text {
  color: #ef4444;
}

/* ===== STATS SECTION ===== */
.stats-section {
  padding: 80px 20px;
  background: #f9fafb;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s ease;
}

.stats-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.stats-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
}

.stat-card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(130, 42, 204, 0.15);
}

.stat-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 36px;
  color: white;
}

.stat-card h3 {
  font-size: 2.5rem;
  font-weight: 800;
  color: #822acc;
  margin: 0 0 10px 0;
}

.stat-card p {
  font-size: 1rem;
  color: #b1aeae;
  margin: 0;
}

/* ===== FEATURES SECTION ===== */
.features-section {
  padding: 100px 20px;
  background: white;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s ease;
}

.features-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.section-title {
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 800;
  text-align: center;
  margin-bottom: 60px;
  color: #1f2937;
  line-height: 1.2;
}

.features-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 30px;
}

.feature-card {
  background: white;
  padding: 40px;
  border-radius: 24px;
  border: 2px solid #f3f4f6;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #822acc, #e0d824);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.feature-card:hover {
  border-color: #822acc;
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(130, 42, 204, 0.15);
}

.feature-card:hover::before {
  transform: scaleX(1);
}

.highlight-card {
  background: linear-gradient(135deg, #822acc05, #e0d82405);
  border-color: #822acc;
}

.feature-icon {
  width: 70px;
  height: 70px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  margin-bottom: 24px;
}

.gradient-1 { background: linear-gradient(135deg, #822acc, #651e9e); }
.gradient-2 { background: linear-gradient(135deg, #10b981, #059669); }
.gradient-3 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.gradient-4 { background: linear-gradient(135deg, #e0d824, #f5e84a); }
.gradient-5 { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.gradient-6 { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.feature-card h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.feature-card p {
  font-size: 1rem;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.feature-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  color: white;
  padding: 6px 16px;
  border-radius: 50px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

.feature-badge i {
  font-size: 12px;
}

.feature-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #822acc;
  font-weight: 600;
  margin-top: auto;
}

.feature-action i {
  transition: transform 0.3s ease;
}

.feature-card:hover .feature-action i {
  transform: translateX(5px);
}

/* ===== CTA SECTION ===== */
.cta-section {
  position: relative;
  padding: 100px 20px;
  background: linear-gradient(135deg, #822acc 0%, #651e9e 100%);
  overflow: hidden;
}

.cta-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
  max-width: 800px;
  margin: 0 auto;
}

.cta-content h2 {
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 800;
  margin-bottom: 20px;
}

.cta-content p {
  font-size: 1.25rem;
  margin-bottom: 40px;
  opacity: 0.9;
}

.cta-button {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #e0d824, #f5e84a);
  color: #4a1575;
  padding: 20px 48px;
  border-radius: 50px;
  font-size: 1.2rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 30px rgba(224, 216, 36, 0.3);
}

.cta-button:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 15px 40px rgba(224, 216, 36, 0.5);
}

.cta-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(224, 216, 36, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: 10%;
  animation-delay: 1s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: -75px;
  animation-delay: 2s;
}

/* ===== ANIMATIONS ===== */
@keyframes fadeSlideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
    text-align: center;
  }

  .hero-text {
    order: 1;
  }

  .hero-visual {
    order: 2;
    height: 400px;
  }

  .cta-primary,
  .cta-secondary {
    width: 100%;
    justify-content: center;
  }

  .quick-stats {
    justify-content: center;
  }

  .hero-cta {
    align-items: center;
  }

  .booklets-list {
    width: 100%;
  }

  .booklets-header {
    margin-bottom: 16px;
  }

  .booklets-header h3 {
    font-size: 1.3rem;
  }

  .booklet-card {
    padding: 16px;
  }

  .booklet-icon {
    width: 45px;
    height: 45px;
    font-size: 20px;
  }

  .booklet-label {
    font-size: 1rem;
  }

  .booklet-amount {
    font-size: 1.2rem;
  }
}

@media (max-width: 768px) {
  .hero-section {
    min-height: auto;
    padding: 40px 20px;
  }

  .hero-title {
    font-size: 2rem;
  }

  .dashboard-button {
    padding: 10px 20px;
    font-size: 0.9rem;
  }

  .dashboard-button span {
    display: none;
  }

  .dashboard-button i {
    margin: 0;
  }

  .hero-description {
    font-size: 1rem;
  }

  .floating-card {
    padding: 12px 16px;
  }

  .card-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .card-value {
    font-size: 16px;
  }

  .center-circle {
    width: 150px;
    height: 150px;
  }

  .center-circle i {
    font-size: 60px;
  }

  .booklets-list {
    width: 100%;
  }

  .booklets-header h3 {
    font-size: 1.2rem;
  }

  .add-booklet-btn {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .booklets-grid {
    max-height: 300px;
    gap: 10px;
  }

  .booklet-card {
    padding: 14px;
    gap: 12px;
  }

  .booklet-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .booklet-label {
    font-size: 0.95rem;
  }

  .booklet-amount {
    font-size: 1.1rem;
  }

  .booklet-arrow {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .cta-primary,
  .cta-secondary {
    padding: 14px 28px;
    font-size: 1rem;
  }

  .stat-item {
    padding: 10px 16px;
    font-size: 0.9rem;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .stats-section,
  .features-section,
  .cta-section {
    padding: 60px 20px;
  }
}
</style>
