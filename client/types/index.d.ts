declare global {
  // declare your types here
  interface SheetDTO {
    id: string
    label: string
    value: number
    isIncome: boolean
    date: Date
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
    regularTransactionId?: string | null
  }
  interface TransactionResultDTO {
    id: string | null
    label: string
    value: number
    isIncome: boolean
    date: string
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
    bookletAmount: string
    regularTransactionId?: string | null
  }

  interface BookletDTO {
    id: number | undefined
    amount: number
    label: string
    transactions: Array<SheetDTO>
    currency?: string
  }

  type OnlyBookletInfo = Pick<BookletDTO, 'id' | 'amount' | 'label' | 'currency'>

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
    tagId: string | undefined
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
    regularTransactionId?: string | null
  }

  interface BookletCreationRequest {
    label: string
    amount: number
    currency: string
  }
  interface RegularTransactionDTO {
    id: string
    label: string
    startDate: Date | string
    value: number
    isIncome: boolean
    regularity: string
    tagDTO: TagDTO
    frequencyProperty: FrequencyPropertyDTO
    bookletIds?: string[]
  }

  interface FrequencyPropertyDTO {
    type: 'FOREVER' | 'UNTIL_DATE' | 'TIMES'
    untilDate?: Date | string
    times?: number
  }

  interface RecurrenceRuleDTO {
    type: string
    dayOfMonth?: number
  }

  interface MonthlyTransactionCreationRequest {
    label: string
    value: number
    isIncome: boolean
    startDate: Date
    tagDTO?: TagDTO | null
    frequencyProperty: FrequencyPropertyDTO
    repeatDay: number | null
    bookletIds: string[]
  }

  interface UpdateRegularTransactionRequest {
    id: string
    label: string
    value: number
    startDate: Date | string
    isIncome: boolean
    tagDTO: TagDTO
    frequencyProperty: FrequencyPropertyDTO
    bookletIds: string[]
    recurrenceRule: RecurrenceRuleDTO
  }

  interface RegularTransactionsDeletionRequest {
    transactionIds: string[]
  }
  interface BookletReport {
    label: string
    transactions: TransactionResultDTO[]
    realSold: string
    previewSold: string
  }

  interface MonthlyAccountStatsDTO {
    bookletId: string
    bookletLabel: string
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
    tagId: string | null
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
    totalBooklets: number
  }

  interface PrevisionalTransactionsDTO {
    transactions: StatsTransactionDTO[]
    groupedByBooklet: Record<string, StatsTransactionDTO[]>
    totalAmount: string
    totalIncome: string
    totalExpenses: string
    regularTransactions: StatsTransactionDTO[]
    nonRegularTransactions: StatsTransactionDTO[]
    totalRegularAmount: string
    totalNonRegularAmount: string
    startDate: Date
    endDate: Date
  }

  interface StatsTransactionDTO {
    id: string | null
    label: string
    amount: string
    isIncome: boolean
    date: Date
    tag: string
    regularTransactionId?: string | null
  }

  interface UserSettingsDTO {
    projectionWindowDays: number
    bookletCycles: BookletMonthlyCycleDTO[]
  }

  interface BookletMonthlyCycleDTO {
    bookletId: string
    label: string
    monthlyPeriodStartDay: number
    monthlyPeriodEndDay?: number | null
  }

  interface UserSettingsUpdateDTO {
    projectionWindowDays: number
    bookletCycles: BookletMonthlyCycleUpdateDTO[]
  }

  interface BookletMonthlyCycleUpdateDTO {
    bookletId: string
    monthlyPeriodStartDay: number
    monthlyPeriodEndDay?: number | null
  }

  interface CsvValidationReportDTO {
    totalLines: number
    validLines: number
    errors: CsvValidationIssueDTO[]
    warnings: CsvValidationIssueDTO[]
    suggestions: string[]
    hasErrors: boolean
    canImport: boolean
  }

  interface CsvValidationIssueDTO {
    lineNumber: number
    type: string
    message: string
    detectedValue: string | null
  }

  interface CsvImportResultDTO {
    successCount: number
    failedCount: number
    totalProcessed: number
    hasErrors: boolean
    transactions: CsvTransactionDTO[]
    errors: CsvLineErrorDTO[]
  }

  interface CsvLineErrorDTO {
    lineNumber: number
    errors: string[]
  }

  interface CsvTransactionDTO {
    id: string | null
    label: string
    date: string
    amount: string
    isIncome: boolean
    tag: string | null
  }
}

export {}
