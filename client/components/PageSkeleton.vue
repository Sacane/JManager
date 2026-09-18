<script setup lang="ts">
/**
 * Loading placeholder shaped like the content it stands for.
 *
 * Replaces the page-level spinners, which left a screen empty and then revealed everything at
 * once. It is for content being loaded, not for an action in progress: a button that is saving
 * keeps its spinner.
 *
 * The shapes are hidden from assistive technology and the `label` is announced instead — a
 * skeleton is otherwise silent for a screen reader user.
 */
interface Props {
  variant: 'dashboard' | 'list' | 'cards' | 'form'
  /** Announced to assistive technology, and kept for anyone searching the page for it. */
  label: string
  /** Rows, cards or fields to draw. Ignored by the dashboard variant, whose layout is fixed. */
  count?: number
}

const props = withDefaults(defineProps<Props>(), { count: 5 })
</script>

<template>
  <div data-test="page-skeleton" class="page-skeleton" role="status" aria-live="polite" aria-busy="true">
    <span class="sr-only">{{ props.label }}</span>

    <div data-test="skeleton-shapes" aria-hidden="true">
      <template v-if="props.variant === 'dashboard'">
        <div class="skeleton-kpis">
          <div v-for="index in 4" :key="`kpi-${index}`" data-test="skeleton-kpi" class="skeleton-panel">
            <div class="bone bone--line bone--short" />
            <div class="bone bone--figure" />
          </div>
        </div>
        <div class="skeleton-charts">
          <div data-test="skeleton-chart" class="skeleton-panel skeleton-panel--wide">
            <div class="bone bone--line bone--short" />
            <div class="bone bone--chart" />
          </div>
          <div data-test="skeleton-chart" class="skeleton-panel">
            <div class="bone bone--line bone--short" />
            <div class="bone bone--chart" />
          </div>
        </div>
      </template>

      <div v-else-if="props.variant === 'cards'" class="skeleton-cards">
        <div v-for="index in props.count" :key="`card-${index}`" data-test="skeleton-card" class="skeleton-panel">
          <div class="bone bone--line bone--medium" />
          <div class="bone bone--figure" />
          <div class="bone bone--line bone--short" />
        </div>
      </div>

      <div v-else-if="props.variant === 'form'" class="skeleton-form">
        <div v-for="index in props.count" :key="`field-${index}`" data-test="skeleton-field" class="skeleton-field">
          <div class="bone bone--line bone--short" />
          <div class="bone bone--input" />
        </div>
      </div>

      <div v-else class="skeleton-list">
        <div v-for="index in props.count" :key="`row-${index}`" data-test="skeleton-row" class="skeleton-row">
          <div class="bone bone--dot" />
          <div class="bone bone--line bone--long" />
          <div class="bone bone--line bone--amount" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-skeleton {
  width: 100%;
}

.skeleton-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  border: 1px solid var(--card-border);
  border-radius: 1rem;
  background: var(--card-bg);
}

.skeleton-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.skeleton-charts {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
}

.skeleton-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 1rem;
}

.skeleton-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.skeleton-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.9rem 0.25rem;
  border-bottom: 1px solid var(--card-border);
}

.bone {
  border-radius: 0.4rem;
  background: linear-gradient(90deg, var(--bg-tertiary) 25%, var(--bg-secondary) 50%, var(--bg-tertiary) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.bone--line { height: 0.8rem; }
.bone--short { width: 35%; }
.bone--medium { width: 60%; }
.bone--long { flex: 1; }
.bone--amount { width: 5rem; flex-shrink: 0; }
.bone--figure { height: 2rem; width: 55%; }
.bone--chart { height: 14rem; width: 100%; }
.bone--input { height: 2.6rem; width: 100%; }
.bone--dot { width: 2rem; height: 2rem; border-radius: 999px; flex-shrink: 0; }

@keyframes skeleton-shimmer {
  from { background-position: 100% 0; }
  to { background-position: -100% 0; }
}

/* A shimmer is decoration; for someone who asked the system to reduce motion it is noise. */
@media (prefers-reduced-motion: reduce) {
  .bone { animation: none; }
}

@media (max-width: 768px) {
  .skeleton-charts { grid-template-columns: 1fr; }
}
</style>
