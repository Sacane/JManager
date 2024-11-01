<script setup lang="ts">
import useAuth from '@/composables/useAuth'

definePageMeta({
  layout: 'sidebar-layout',
})
const auth = useAuth()
</script>

<template>
  <div class="self-center h-full w-full flex flex-row mt-50px">
    <div class="flex flex-col division w-full max-w[80%] gap-15px ">
      <div class="line w-full flex flex-row gap-20px">
        <div class="content w-[15%]">
          <div class="user-icon-container">
            <i class="pi pi-user user-icon" />
          </div>
          <h2 class="ml-6">
            {{ auth.user.username }}
          </h2>
        </div>
        <div class="content w-[85%] card">
          <div class="card-header">
            <div class="title-container">
              <h1>Livrets</h1>
            </div>
          </div>
          <div class="card-body">
            <i class="pi pi-plus icon-large" />
            <p>Créer votre premier livret</p>
          </div>
        </div>
      </div>
      <div class="line w-full flex flex-row gap-20px">
        <div class="content w-[67%]">
          <h1>Transactions</h1>
        </div>
        <div class="content w-[33%]">
          <div class="card-header">
            <div class="title-container">
              <h1>Tags</h1>
            </div>
          </div>
          <div class="card-body">
            <i class="pi pi-plus icon-large" />
            <p>Créer votre premier tag personnel</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.division {
  transition: ease 1s;
}
.content {
  background: linear-gradient(135deg, #2C3E50, #1F2D3A);
  box-shadow: 5px 5px 15px black;
  border-radius: 20px;
  transition: ease 0.5s;
  color: #fff;
  &:hover {
    transform: scale(1.05);
  }
}
.user-icon-container {
  margin-top: 10px;
  margin-left: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  border-radius: 50%;
  width: 100px;
  height: 100px;
}

.user-icon {
  color: rgba(206, 20, 20, 0.35);
  font-size: 50px;
}

.card {
  background: linear-gradient(135deg, #2C3E50, #1F2D3A);
  border-radius: 12px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  font-family: Arial, sans-serif;
}

/* Header de la carte */
.card-header {
  display: flex;
  align-items: center;
}

.title-container h3 {
  margin: 0;
  font-size: 16px;
}

.title-container p {
  margin: 0;
  font-size: 12px;
  color: #b0b0b0;
}

/* Corps de la carte */
.card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

/* Icône de cloud (PrimeIcons ou FontAwesome par exemple) */
.icon-large {
  font-size: 36px;
  color: #4EA8DE;
}

.card-body p {
  margin-top: 8px;
  font-size: 14px;
  color: #d0d0d0;
}
</style>
