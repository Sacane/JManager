<template>
  <div class="w-full flex flex-col md:flex-row justify-center items-center md:justify-evenly">
    <div class="flex flex-col w-full md:w-auto p-10 rounded-lg border border-gray-400">
      <h1 class="text-3xl font-bold">
        Se connecter
      </h1>
      <form class="flex flex-col gap-2 p-3 border-rd-20px border-none">
        <label for="email">Nom d'utilisateur</label>
        <input id="email" type="email" class="border border-gray-400 rounded-lg p-2">
        <label for="password">Password</label>
        <input id="password" type="password" class="border border-gray-400 rounded-lg mb-4 p-2">
        <button class="bg-#7F52FF text-white p-7px rounded-2">
          Login
        </button>
      </form>
    </div>
    <div class="flex flex-col w-full md:w-auto p-10 rounded-lg border border-gray-400 mt-4 md:mt-0">
      <h1 class="text-3xl font-bold">
        S'enregistrer
      </h1>
      <form class="flex flex-col gap-2 p-10 border-rd-20px border-none">
        <label for="name">Nom d'utilisateur</label>
        <input id="name" type="text" class="border border-gray-400 rounded-lg">
        <label for="email">Email</label>
        <input id="email" type="email" class="border border-gray-400 rounded-lg">
        <label for="password">Password</label>
        <input id="password" type="password" class="border border-gray-400 rounded-lg mb-4">
        <button class="bg-#7F52FF text-white p-7px rounded-2">
          S'enregistrer
        </button>
      </form>
    </div>
  </div>
</template>
