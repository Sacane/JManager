declare global {
  // declare your types here
  interface SheetDTO {
    id: number
    label: string
    value: string
    isIncome: boolean
    date: string
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
  }
  interface TransactionResultDTO {
    id: string
    label: string
    value: string
    isIncome: boolean
    date: string
    color: ColorDTO
    tagDTO: TagDTO
    isPreview: boolean
    accountAmount: string
    accountPreviewAmount: string
  }

  interface AccountDTO {
    id: number | undefined
    amount: string
    labelAccount: string
    sheets: Array<SheetDTO>
    previewAmount: string
  }

  interface SheetAverageDTO {
    sheets: SheetDTO[]
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
    id?: string
    label: string
    value: string
    isIncome: boolean
    date: string
    tagDTO: TagDTO
    isPreview: boolean
  }

  interface BookletCreationRequest {
    id: number
    labelAccount: string
    amount: number
    currency: string
  }
}
