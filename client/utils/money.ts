const FRENCH_AMOUNT = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** "+1 234,50 €" for an income, "-60,00 €" for an expense. */
export function formatSignedAmount(value: number | string, isIncome: boolean): string {
  const amount = Math.abs(typeof value === 'string' ? Number.parseFloat(value) : value)
  return `${isIncome ? '+' : '-'}${FRENCH_AMOUNT.format(amount)} €`
}
