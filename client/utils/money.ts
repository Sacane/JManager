const FRENCH_AMOUNT = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** "2 940,00 €" for a balance, "-12,50 €" once it goes below zero: a level, not a movement. */
export function formatBalance(value: number | string): string {
  const amount = typeof value === 'string' ? Number.parseFloat(value) : value
  return `${FRENCH_AMOUNT.format(amount)} €`
}

/** "+1 234,50 €" for an income, "-60,00 €" for an expense. */
export function formatSignedAmount(value: number | string, isIncome: boolean): string {
  const amount = Math.abs(typeof value === 'string' ? Number.parseFloat(value) : value)
  return `${isIncome ? '+' : '-'}${FRENCH_AMOUNT.format(amount)} €`
}
