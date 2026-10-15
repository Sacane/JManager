import { normalizedSearch } from '~/utils/labelSearch'

/** A transaction as the search endpoint returns it. */
export interface FoundTransactionDTO {
  id: string | null
  label: string
  value: number | string
  isIncome: boolean
  /** "yyyy-MM-dd" */
  date: string
  isPreview: boolean
  tagDTO?: TagDTO | null
  regularTransactionId?: string | null
}

export interface SearchedTransactionDTO {
  transaction: FoundTransactionDTO
  bookletId: string
  bookletLabel: string
}

export interface RegularTransactionMatchDTO {
  regularTransactionId: string
  label: string
  bookletId: string
  bookletLabel: string
  occurrenceCount: number
  firstDate: string
  lastDate: string
  /** Newest first. */
  occurrences: SearchedTransactionDTO[]
}

export interface MonthMatchesDTO {
  /** "yyyy-MM" */
  month: string
  /** Newest first. */
  transactions: SearchedTransactionDTO[]
}

export interface TransactionSearchResponse {
  from: string
  to: string
  regulars: RegularTransactionMatchDTO[]
  months: MonthMatchesDTO[]
  /** Whether searching further back may find more. */
  hasOlderTransactions: boolean
}

/** The search over every booklet of the user (UX-22, part B). */
export default function useTransactionSearch() {
  const { get } = useQuery()

  async function searchTransactions(search: string, months: number, bookletId?: string): Promise<TransactionSearchResponse> {
    return get('transaction/search', {
      q: normalizedSearch(search),
      months,
      ...(bookletId ? { bookletId } : {}),
    })
  }

  return { searchTransactions }
}
