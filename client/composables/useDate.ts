import { parse } from 'date-fns'

export default function useDate() {
  function translate(month: string): string {
    switch (month) {
      case 'JANUARY': return 'JANVIER'
      case 'FEBRUARY': return 'FEVRIER'
      case 'MARCH': return 'MARS'
      case 'APRIL': return 'AVRIL'
      case 'MAY': return 'MAI'
      case 'JUNE': return 'JUIN'
      case 'JULY': return 'JUILLET'
      case 'AUGUST': return 'AOUT'
      case 'SEPTEMBER': return 'SEPTEMBRE'
      case 'OCTOBER': return 'OCTOBRE'
      case 'NOVEMBER': return 'NOVEMBRE'
      case 'DECEMBER': return 'DECEMBRE'
    }
    return ''
  }

  function englishMonth(month: string): string {
    switch (month) {
      case 'JANVIER': return 'JANUARY'
      case 'FEVRIER': return 'FEBRUARY'
      case 'MARS': return 'MARCH'
      case 'AVRIL': return 'APRIL'
      case 'MAI': return 'MAY'
      case 'JUIN': return 'JUNE'
      case 'JUILLET': return 'JULY'
      case 'AOUT': return 'AUGUST'
      case 'SEPTEMBRE': return 'SEPTEMBER'
      case 'OCTOBRE': return 'OCTOBER'
      case 'NOVEMBRE': return 'NOVEMBER'
      case 'DECEMBRE': return 'DECEMBER'
    }
    return ''
  }

  function monthFromNumber(num: number): string | undefined {
    switch (num) {
      case 1: return 'JANUARY'
      case 2: return 'FEBRUARY'
      case 3: return 'MARCH'
      case 4: return 'APRIL'
      case 5: return 'MAY'
      case 6: return 'JUNE'
      case 7: return 'JULY'
      case 8: return 'AUGUST'
      case 9: return 'SEPTEMBER'
      case 10: return 'OCTOBER'
      case 11: return 'NOVEMBER'
      case 12: return 'DECEMBER'
      default: return undefined
    }
  }

  function numberFromMonth(month: string): number | undefined {
    switch (month) {
      case 'JANUARY': return 1
      case 'FEBRUARY': return 2
      case 'MARCH': return 3
      case 'APRIL': return 4
      case 'MAY': return 5
      case 'JUNE': return 6
      case 'JULY': return 7
      case 'AUGUST': return 8
      case 'SEPTEMBER': return 9
      case 'OCTOBER': return 10
      case 'NOVEMBER': return 11
      case 'DECEMBER': return 12
      default: return undefined
    }
  }

  const months = [
    'JANUARY',
    'FEBRUARY',
    'MARCH',
    'APRIL',
    'MAY',
    'JUNE',
    'JULY',
    'AUGUST',
    'SEPTEMBER',
    'OCTOBER',
    'NOVEMBER',
    'DECEMBER',
  ]

  function dateFromString(dateString: string): Date {
    return parse(dateString, 'yyyy-MM-dd', new Date())
  }

  function formatDate(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate())
  }

  function formattedDateString(date: Date): string {
    const day = String(date.getDate()).padStart(2, '0')
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const year = date.getFullYear()

    return `${year}-${month}-${day}`
  }

  function frequencyToString(frequency: Frequency): string {
    switch (frequency) {
      case 'DAILY':
        return 'Tous les jours'
      case 'WEEKLY':
        return 'Chaque semaine'
      case 'MONTHLY':
        return 'Tous les mois'
      case 'YEARLY':
        return 'Chaque année'
    }
  }
  function strToFrequency(str: string): string | undefined {
    switch (str) {
      case 'Tous les jours':
        return 'DAILY'
      case 'Chaque semaine':
        return 'WEEKLY'
      case 'Tous les mois':
        return 'MONTHLY'
      case 'Chaque année':
        return 'YEARLY'
    }
  }

  return { months, translate, monthFromNumber, dateFromString, formattedDateString, englishMonth, frequencyToString, strToFrequency, numberFromMonth, formatDate }
}
