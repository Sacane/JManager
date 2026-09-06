<script setup lang="ts">
interface Props {
  /** Required: the label of the surrounding form field points at it. */
  id: string
  placeholder?: string
  /** UX-46 relies on this reaching the input, so it is forwarded explicitly. */
  autocomplete?: string
  maxlength?: number
  disabled?: boolean
  /** Extra classes for the input itself, for pages that style their own fields. */
  inputClass?: string
  ariaLabel?: string
  dataTest?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: undefined,
  autocomplete: undefined,
  maxlength: 100,
  disabled: false,
  inputClass: '',
  ariaLabel: undefined,
  dataTest: undefined,
})

const model = defineModel<string>({ default: '' })

const isRevealed = ref(false)

// The control carries no text, so its accessible name is all a screen reader gets. It names the
// action it performs rather than the current state, which is what a user needs to decide.
const toggleLabel = computed(() =>
  isRevealed.value ? 'Masquer le mot de passe' : 'Afficher le mot de passe',
)

function toggleVisibility(): void {
  isRevealed.value = !isRevealed.value
}
</script>

<template>
  <div class="password-field">
    <div class="password-field__control">
      <input
        :id="props.id"
        v-model="model"
        :type="isRevealed ? 'text' : 'password'"
        class="p-inputtext p-component password-field__input" :class="[props.inputClass]"
        :placeholder="props.placeholder"
        :autocomplete="props.autocomplete"
        :maxlength="props.maxlength"
        :disabled="props.disabled"
        :aria-label="props.ariaLabel"
        :data-test="props.dataTest"
      >
      <button
        type="button"
        class="password-field__toggle"
        data-test="toggle-password-visibility"
        :aria-label="toggleLabel"
        :aria-pressed="isRevealed"
        :disabled="props.disabled"
        :title="toggleLabel"
        @click="toggleVisibility"
      >
        <i :class="isRevealed ? 'pi pi-eye-slash' : 'pi pi-eye'" aria-hidden="true" />
      </button>
    </div>

    <!-- UX-27 will hang the password rules and the strength indicator here. -->
    <slot />
  </div>
</template>

<style scoped>
.password-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  width: 100%;
}

.password-field__control {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.password-field__input {
  width: 100%;
  /* Room for the toggle, so a long value never runs under it. */
  padding-right: 2.75rem;
}

.password-field__toggle {
  position: absolute;
  right: 0.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: none;
  border-radius: 0.5rem;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.password-field__toggle:hover:not(:disabled) {
  color: var(--primary);
  background: var(--card-hover-bg);
}

.password-field__toggle:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
