<script setup lang="ts">
const props = defineProps<{
  group: TagGroupItem
  disabled: boolean
  deleting: boolean
  selected: boolean
  selectedChildIds: string[]
}>()

const children = computed(() => props.group.children)
const { orderedItems, draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop, onDragEnd } = useSubTagOrder(props.group.id, children)

const emit = defineEmits<{
  (e: 'edit', tag: TagDisplayItem): void
  (e: 'delete', tag: TagDisplayItem): void
  (e: 'toggle-select', tag: TagDisplayItem): void
  (e: 'toggle-select-child', tag: TagDisplayItem): void
}>()
</script>

<template>
  <div
    class="tag-card"
    :class="{ 'tag-card-personal': !props.group.isDefault, 'tag-card-selected': selected }"
    :style="{ '--tag-color': props.group.color }"
  >
    <div class="tag-color-band" :style="{ backgroundColor: props.group.color }" />

    <div class="tag-content">
      <div class="tag-info">
        <div class="tag-header">
          <Checkbox
            v-if="!props.group.isDefault"
            :binary="true"
            :model-value="selected"
            class="tag-select-checkbox"
            aria-label="Sélectionner ce tag"
            @update:model-value="() => emit('toggle-select', props.group)"
          />
          <div class="tag-label-wrapper">
            <h3 class="tag-label">
              {{ props.group.label }}
            </h3>
            <Tag
              :value="props.group.isDefault ? 'Par défaut' : 'Personnel'"
              :severity="props.group.isDefault ? 'info' : 'success'"
              class="tag-badge"
            />
          </div>
        </div>

        <div class="color-preview">
          <div class="color-circle" :style="{ backgroundColor: props.group.color }" />
          <span class="color-label">{{ props.group.color }}</span>
        </div>
      </div>

      <div v-if="props.group.children.length > 0" class="sub-tags-section">
        <p class="sub-tags-header">
          <i class="pi pi-sitemap" />
          Sous-tags ({{ props.group.children.length }})
        </p>
        <div class="sub-tags-list">
          <div
            v-for="(child, index) in orderedItems"
            :key="child.id"
            class="sub-tag-chip"
            :class="{
              'sub-tag-chip-selected': selectedChildIds.includes(child.id),
              'is-dragging': draggedIndex === index,
              'drag-over': dragOverIndex === index && draggedIndex !== index,
            }"
            :style="{ borderLeftColor: child.color }"
            :draggable="props.group.children.length > 1"
            @dragstart="onDragStart($event, index)"
            @dragover="onDragOver($event, index)"
            @drop="onDrop($event, index)"
            @dragend="onDragEnd"
          >
            <button
              v-if="props.group.children.length > 1"
              v-tooltip.top="'Réorganiser'"
              class="sub-tag-drag-handle"
              @click.stop
            >
              <i class="pi pi-bars" />
            </button>
            <Checkbox
              v-if="!child.isDefault"
              :binary="true"
              :model-value="selectedChildIds.includes(child.id)"
              class="sub-tag-checkbox"
              aria-label="Sélectionner ce sous-tag"
              @update:model-value="() => emit('toggle-select-child', child)"
            />
            <div class="color-circle-sm" :style="{ backgroundColor: child.color }" />
            <span class="sub-tag-label">{{ child.label }}</span>
            <div v-if="!child.isDefault" class="sub-tag-actions">
              <Button
                v-tooltip.top="'Modifier'"
                icon="pi pi-pencil"
                rounded
                text
                severity="secondary"
                size="small"
                :disabled="disabled"
                aria-label="Modifier"
                @click="emit('edit', child)"
              />
              <Button
                v-tooltip.top="'Supprimer'"
                icon="pi pi-trash"
                rounded
                text
                severity="danger"
                size="small"
                :disabled="disabled"
                aria-label="Supprimer"
                @click="emit('delete', child)"
              />
            </div>
          </div>
        </div>
      </div>

      <div v-if="!props.group.isDefault" class="tag-actions">
        <Button
          v-tooltip.top="'Modifier'"
          icon="pi pi-pencil"
          rounded
          text
          severity="secondary"
          :disabled="disabled"
          aria-label="Modifier"
          @click="emit('edit', props.group)"
        />
        <Button
          v-tooltip.top="'Supprimer'"
          icon="pi pi-trash"
          rounded
          text
          severity="danger"
          :loading="deleting"
          :disabled="disabled"
          aria-label="Supprimer"
          @click="emit('delete', props.group)"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.tag-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px var(--shadow-sm), 0 1px 2px var(--shadow-sm);
  transition: all 0.3s ease;
  border: 1px solid var(--card-border);

  &.tag-card-selected {
    border-color: var(--p-primary-color, #6366f1);
    box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2), 0 1px 3px var(--shadow-sm);
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px var(--shadow-md), 0 4px 8px var(--shadow-sm);
    border-color: var(--border-light);
  }

  &.tag-card-personal:hover {
    .tag-actions {
      opacity: 1;
      pointer-events: all;
    }
  }
}

.tag-color-band {
  height: 6px;
  width: 100%;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.2) 100%);
  }
}

.tag-content {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.tag-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.tag-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.tag-label-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  min-width: 0;
}

.tag-label {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.tag-badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.color-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  box-shadow: 0 2px 8px var(--shadow-sm);
  flex-shrink: 0;
}

.color-label {
  font-family: 'SF Mono', 'Monaco', 'Consolas', 'Courier New', monospace;
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  font-weight: 500;
}

.tag-actions {
  display: flex;
  gap: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-color);
  opacity: 0.7;
  transition: opacity 0.3s ease;

  @media (max-width: 768px) {
    opacity: 1;
  }
}

.sub-tags-section {
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-color);
}

.sub-tags-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.5rem 0;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.sub-tags-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sub-tag-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.6rem;
  border-radius: 6px;
  border-left: 3px solid;
  background: var(--bg-tertiary);
  transition: background 0.2s ease, opacity 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    background: var(--bg-hover, var(--bg-tertiary));

    .sub-tag-actions {
      opacity: 1;
    }

    .sub-tag-drag-handle {
      opacity: 1;
    }
  }

  &.is-dragging {
    opacity: 0.4;
    transform: scale(0.98);
    box-shadow: none;
  }

  &.drag-over {
    box-shadow: 0 -2px 0 0 var(--p-primary-color, #6366f1);
    background: color-mix(in srgb, var(--p-primary-color, #6366f1) 6%, var(--bg-tertiary));
  }
}

.sub-tag-drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: grab;
  border-radius: 4px;
  transition: all 0.2s ease;
  padding: 0;
  opacity: 0;
  flex-shrink: 0;

  @media (max-width: 768px) {
    opacity: 1;
  }

  &:active {
    cursor: grabbing;
  }

  &:hover {
    color: var(--primary, var(--p-primary-color));
    background: color-mix(in srgb, var(--p-primary-color, #6366f1) 10%, transparent);
  }

  i {
    font-size: 0.75rem;
  }
}

.color-circle-sm {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1.5px solid var(--border-color);
  flex-shrink: 0;
}

.sub-tag-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary);
  flex: 1;
}

.sub-tag-actions {
  display: flex;
  gap: 0.25rem;
  opacity: 0;
  transition: opacity 0.2s ease;

  @media (max-width: 768px) {
    opacity: 1;
  }
}

.sub-tag-chip-selected {
  background: color-mix(in srgb, var(--p-primary-color, #6366f1) 8%, var(--bg-tertiary));
  outline: 1px solid rgba(99, 102, 241, 0.3);
  outline-offset: -1px;
}

.tag-select-checkbox,
.sub-tag-checkbox {
  flex-shrink: 0;
}
</style>
