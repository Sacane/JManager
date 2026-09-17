<script setup lang="ts">
/**
 * Validation message attached to the field that caused it.
 *
 * Renders nothing when there is no message, so a form can bind it unconditionally. It is announced
 * as an alert: a user who submits with the keyboard never sees a message that only appears
 * visually.
 */
interface Props {
  message?: string | null
  dataTest?: string
}

const props = withDefaults(defineProps<Props>(), {
  message: null,
  dataTest: undefined,
})
</script>

<template>
  <p
    v-if="props.message"
    :data-test="props.dataTest"
    role="alert"
    class="field-error"
  >
    <i class="pi pi-exclamation-circle" aria-hidden="true" />
    {{ props.message }}
  </p>
</template>

<style scoped>
.field-error {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--danger);
}

.field-error i {
  font-size: 0.75rem;
}
</style>
