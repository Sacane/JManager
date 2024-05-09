declare global {
  // declare your types here
  interface SheetDTO {
    id: number
    label: string
    expenses: string
    income: string
    date: string
    accountAmount: string
    color: ColorDTO
  }

  interface AccountDTO {
    id: number | undefined
    amount: string
    labelAccount: string
    sheets: Array<SheetDTO>
  }

  interface UserAccountDTO {
    id: number
    amount: string
    labelAccount: string
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
}
