<template>
  <header class="bg-gray-200 py-4 border-b">
    <h1 class="text-2xl font-bold text-center">
      Bienvenue sur JManager
    </h1>
  </header>
  <div class="mt-3xl">
    <slot />
  </div>
  <footer class="absolute bottom-0">
    @copyright 2023
  </footer>
</template>
