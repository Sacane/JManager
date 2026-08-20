<script setup lang="ts">
import type { DiagnosticContext } from '~/utils/errorCodeMap'
import { buildDiagnosticText } from '~/utils/errorCodeMap'

interface ToastMessage extends DiagnosticContext {
  summary?: string
  detail?: string
  severity?: string
}

type CopyState = 'idle' | 'copied' | 'error'

const copyStates = reactive<Record<string, CopyState>>({})

const severityIcon: Record<string, string> = {
  success: 'pi pi-check-circle',
  info: 'pi pi-info-circle',
  warn: 'pi pi-exclamation-triangle',
  error: 'pi pi-times-circle',
}

const severityColor: Record<string, string> = {
  success: 'text-green-400',
  info: 'text-blue-400',
  warn: 'text-yellow-400',
  error: 'text-red-400',
}

function copyButtonLabel(requestId: string): string {
  const state = copyStates[requestId] ?? 'idle'
  if (state === 'copied') return 'Copié !'
  if (state === 'error') return 'Échec de la copie'
  return 'Copier les infos de débogage'
}

async function copyDiagnostic(msg: ToastMessage) {
  if (!msg.requestId) return
  const requestId = msg.requestId
  const text = buildDiagnosticText(msg)
  try {
    await navigator.clipboard.writeText(text)
    copyStates[requestId] = 'copied'
  } catch {
    copyStates[requestId] = 'error'
  }
  setTimeout(() => {
    delete copyStates[requestId]
  }, 2000)
}
</script>

<template>
  <Toast>
    <template #message="{ message: rawMsg }">
      <template v-for="(msg, index) in [(rawMsg as ToastMessage)]" :key="index">
        <div class="flex items-start gap-2.5 flex-1 min-w-0 py-0.5">
          <i
            v-if="msg.severity && severityIcon[msg.severity]"
            class="text-lg mt-0.5 shrink-0" :class="[severityIcon[msg.severity], severityColor[msg.severity]]"
          />
          <div class="flex flex-col gap-1 flex-1 min-w-0">
            <span class="font-semibold text-sm leading-snug">{{ msg.summary }}</span>
            <span class="text-xs opacity-80 leading-relaxed">{{ msg.detail }}</span>
            <button
              v-if="msg.requestId"
              class="self-start mt-1 text-xs font-medium px-2 py-1 rounded border border-white/30 bg-white/10 hover:bg-white/20 transition-colors duration-150 cursor-pointer"
              :aria-label="copyButtonLabel(msg.requestId)"
              @click="copyDiagnostic(msg)"
            >
              {{ copyButtonLabel(msg.requestId) }}
            </button>
          </div>
        </div>
      </template>
    </template>
  </Toast>
</template>
