<script setup lang="ts">
defineProps<{
  inputId?: string
}>()

const model = defineModel<string>({ required: true })
</script>

<template>
  <div class="color-picker-wrapper">
    <input
      :id="inputId"
      v-model="model"
      type="color"
      class="color-picker"
    >
    <InputText
      v-model="model"
      class="color-hex-input"
      placeholder="#000000"
    />
    <div class="color-preview-large" :style="{ backgroundColor: model }" />
  </div>
</template>

<style lang="scss" scoped>
.color-picker-wrapper {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.color-picker {
  width: 60px;
  height: 42px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  background: none;

  &::-webkit-color-swatch-wrapper {
    padding: 2px;
  }

  &::-webkit-color-swatch {
    border: none;
    border-radius: 4px;
  }
}

.color-hex-input {
  flex: 1;
}

.color-preview-large {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  border: 2px solid var(--border-color);
  box-shadow: 0 2px 8px var(--shadow-sm);
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .color-picker {
    width: 48px;
    height: 36px;
  }

  .color-preview-large {
    width: 36px;
    height: 36px;
  }
}
</style>
