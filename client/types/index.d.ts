declare global {
  // declare your types here
  interface SheetDTO {
    id: number
    label: string
    value: number
    isIncome: boolean
    date: string
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
    accountAmount: number
    accountPreviewAmount: number
  }

  interface BookletDTO {
    id: number | undefined
    amount: number
    labelAccount: string
    transactions: Array<SheetDTO>
    previewAmount: number
    currency?: string
  }

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
    date: string
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
    startDate: string
    value: number
    isIncome: boolean
    regularity: string
    tagDTO: TagDTO
    frequencyProperty: FrequencyPropertyDTO
  }

  interface FrequencyPropertyDTO {
    type: 'FOREVER' | 'UNTIL_DATE' | 'TIMES'
    untilDate?: string
    times?: number
  }
  interface MonthlyTransactionCreationRequest {
    label: string
    value: number
    isIncome: boolean
    startDate: string
    tagDTO?: TagDTO | null
    frequencyProperty: FrequencyPropertyDTO
    repeatDay: number | null
  }
  interface BookletReport {
    label: string
    transactions: TransactionResultDTO[]
    realSold: string
    previewSold: string
  }
}

export {}
