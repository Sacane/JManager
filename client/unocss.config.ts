import {
  defineConfig,
  presetAttributify,
  presetIcons,
  presetTypography,
  presetUno,
  presetWebFonts,
  transformerDirectives,
  transformerVariantGroup,
} from 'unocss'

export default defineConfig({
  shortcuts: [
    // --- Layout ---
    ['flex-center', 'flex items-center justify-center'],
    ['grid-center', 'grid place-items-center'],

    // --- Typography scale ---
    // Use these shortcuts to ensure consistent text hierarchy across all pages.
    ['heading-1', 'font-bold text-3xl leading-tight tracking-tight text-[var(--text-primary)]'],
    ['heading-2', 'font-semibold text-2xl leading-snug tracking-tight text-[var(--text-primary)]'],
    ['heading-3', 'font-semibold text-xl leading-snug text-[var(--text-primary)]'],
    ['heading-4', 'font-medium text-lg leading-normal text-[var(--text-secondary)]'],
    ['body-lg', 'text-base font-normal leading-relaxed text-[var(--text-secondary)]'],
    ['body-base', 'text-sm font-normal leading-relaxed text-[var(--text-secondary)]'],
    ['body-sm', 'text-xs font-normal leading-normal text-[var(--text-tertiary)]'],
    ['text-label', 'text-sm font-medium text-[var(--text-primary)]'],
    ['text-caption', 'text-xs font-medium tracking-wider uppercase text-[var(--text-tertiary)]'],
    ['text-muted', 'text-xs font-normal text-[var(--text-muted)]'],

    // --- Legacy (kept for backward compatibility) ---
    ['title', 'font-extrabold text-6xl mb-3'],
    ['list', 'list-disc list-inside'],

    // --- Buttons ---
    ['btn', 'px-4 py-2 border rounded-lg inline-flex items-center justify-center font-medium text-sm cursor-pointer transition-all duration-200 ease-in-out hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50'],
    ['btn-primary', 'btn bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white border-0 shadow-[0_2px_8px_rgba(101,8,204,0.3)] hover:brightness-110 hover:opacity-100'],
    ['btn-outline-primary', 'btn border-[var(--primary)] text-[var(--primary)] bg-transparent hover:bg-[rgba(101,8,204,0.07)]'],
    ['icon-btn', 'inline-flex items-center justify-center w-9 h-9 rounded-lg cursor-pointer select-none opacity-70 hover:opacity-100 hover:text-[var(--primary)] dark:hover:text-[var(--primary-lighter)] transition-all duration-200'],

    // --- Cards ---
    ['card', 'rounded-xl bg-[var(--card-bg)] border border-[var(--card-border)] shadow-sm'],
    ['card-hover', 'card transition-all duration-200 hover:shadow-md hover:-translate-y-0.5'],

    // --- Money ---
    // Income/expense colouring goes through these, never through a raw palette class:
    // the tokens carry a verified >= 4.5:1 contrast in both themes (see UX-11).
    ['amount', 'font-semibold tabular-nums'],
    ['amount-positive', 'amount text-[var(--income)]'],
    ['amount-negative', 'amount text-[var(--expense)]'],
    ['amount-neutral', 'amount text-[var(--text-tertiary)]'],

    // --- Status ---
    ['text-success', 'text-[var(--success)]'],
    ['text-danger', 'text-[var(--danger)]'],
    ['text-warning', 'text-[var(--warning)]'],
    ['text-info', 'text-[var(--info)]'],

    // --- Surfaces ---
    ['surface-page', 'bg-[var(--bg-primary)]'],
    ['surface-section', 'bg-[var(--bg-secondary)]'],
  ],
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.5,
      warn: true,
    }),
    presetTypography(),
    presetWebFonts({
      provider: 'google',
      fonts: {
        // Poppins covers all weights needed by the typography scale.
        // Do not add a separate <link> in nuxt.config.ts — this is the single source.
        sans: [{ name: 'Poppins', weights: ['300', '400', '500', '600', '700', '800'] }],
      },
    }),
  ],
  transformers: [
    transformerDirectives(),
    transformerVariantGroup(),
  ],
  safelist: 'prose prose-sm m-auto text-left'.split(' '),
})
