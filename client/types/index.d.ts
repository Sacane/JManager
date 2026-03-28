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
    accountAmount: string
    regularTransactionId?: string | null
  }

  interface BookletDTO {
    id: number | undefined
    amount: number
    labelAccount: string
    transactions: Array<SheetDTO>
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
    labelAccount: string
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
    value?: number
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
    accountId: string
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
    totalAccounts: number
  }

  interface PrevisionalTransactionsDTO {
    transactions: StatsTransactionDTO[]
    groupedByAccount: Record<string, StatsTransactionDTO[]>
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
    accountCycles: AccountMonthlyCycleDTO[]
  }

  interface AccountMonthlyCycleDTO {
    accountId: string
    label: string
    monthlyPeriodStartDay: number
  }

  interface UserSettingsUpdateDTO {
    projectionWindowDays: number
    accountCycles: AccountMonthlyCycleUpdateDTO[]
  }

  interface AccountMonthlyCycleUpdateDTO {
    accountId: string
    monthlyPeriodStartDay: number
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
