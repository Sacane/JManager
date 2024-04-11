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
    return parse(dateString, 'dd-MM-yyyy', new Date())
  }

  return { months, translate, monthFromNumber, dateFromString }
}
