declare global {
  // declare your types here
  interface SheetDTO {
    id: number
    label: string
    value: number
    isIncome: boolean
    date: Date
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
  }
  interface TransactionResultDTO {
    id: string
    label: string
    value: number
    isIncome: boolean
    date: string
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
    accountAmount: string
    accountPreviewAmount: string
  }

  interface BookletDTO {
    id: number | undefined
    amount: number
    labelAccount: string
    transactions: Array<SheetDTO>
    previewAmount: number
    currency?: string
  }

  type OnlyBookletInfo = Pick<BookletDTO, 'id' | 'amount' | 'labelAccount' | 'currency'>

  interface SheetAverageDTO {
    transactions: TransactionCreationDTO[]
    sum: number
  }
  interface ColorDTO {
    red: number
    green: number
    blue: number
  }
  interface TagDTO {
    tagId: number | undefined
    label: string | undefined
    colorDTO: ColorDTO
    isDefault: boolean
  }

  interface TransactionCreationDTO {
    id: string | null
    label: string
    value: number | null
    isIncome: boolean
    date: Date
    tagDTO: TagDTO
    isPreview: boolean
  }

  interface BookletCreationRequest {
    labelAccount: string
    amount: number
    currency: string
  }
  interface RegularTransactionDTO {
    id: string
    label: string
    startDate: Date
    value: number
    isIncome: boolean
    regularity: string
    tagDTO: TagDTO
    frequencyProperty: FrequencyPropertyDTO
  }

  interface FrequencyPropertyDTO {
    type: 'FOREVER' | 'UNTIL_DATE' | 'TIMES'
    untilDate?: Date
    times?: number
  }
  interface MonthlyTransactionCreationRequest {
    label: string
    value: number
    isIncome: boolean
    startDate: Date
    tagDTO?: TagDTO | null
    frequencyProperty: FrequencyPropertyDTO
    repeatDay: number | null
    bookletIds: number[]
  }
  interface BookletReport {
    label: string
    transactions: TransactionResultDTO[]
    realSold: string
    previewSold: string
  }

  interface MonthlyAccountStatsDTO {
    accountId: number
    accountLabel: string
    year: number
    monthlyData: MonthlyDataDTO[]
  }

  interface MonthlyDataDTO {
    month: number
    income: string
    expenses: string
    balance: string
  }

  interface CategoryDistributionDTO {
    categories: CategoryDataDTO[]
    totalExpenses: string
  }

  interface CategoryDataDTO {
    tagLabel: string
    tagId: number | null
    totalAmount: string
    percentage: number
    transactionCount: number
  }

  interface TrendStatsDTO {
    monthlyTrends: MonthlyTrendDTO[]
  }

  interface MonthlyTrendDTO {
    month: number
    year: number
    income: string
    expenses: string
    balance: string
    cumulativeBalance: string
    totalAccounts: number
  }

  interface PrevisionalTransactionsDTO {
    transactions: StatsTransactionDTO[]
    groupedByAccount: Record<string, StatsTransactionDTO[]>
    totalAmount: string
    totalIncome: string
    totalExpenses: string
    startDate: Date
    endDate: Date
  }

  interface StatsTransactionDTO {
    id: number | null
    label: string
    amount: string
    isIncome: boolean
    date: Date
    tag: string
  }
}

export {}
