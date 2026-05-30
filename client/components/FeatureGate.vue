<script setup lang="ts">
import type { FeatureKey } from '~/constants/featureKeys'

const props = defineProps<{
  feature: FeatureKey
}>()

const { isEnabled } = useFeatureFlags()
const enabled = computed(() => isEnabled(props.feature))
</script>

<template>
  <slot v-if="enabled" />
  <slot v-else name="fallback" />
</template>
