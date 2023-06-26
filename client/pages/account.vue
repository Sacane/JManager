<script setup lang="ts">
definePageMeta({
  layout: 'dashboard',
})
const products = [
  {
    code: 'P1000',
    name: 'Product 1',
    category: 'Category 1',
    quantity: 10,
  },
  {
    code: 'P1001',
    name: 'Product 2',
    category: 'Category 2',
    quantity: 20,
  },
]
</script>

<template>
  <PDataTable :value="products" table-style="min-width: 50rem">
    <PColumn field="code" header="Code" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
    <PColumn field="name" header="Name" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
    <PColumn field="category" header="Category" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
    <PColumn field="quantity" header="Quantity" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
  </PDataTable>
</template>
