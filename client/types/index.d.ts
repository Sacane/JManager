

export interface SheetDTO{
    id: number,
    label: string,
    expenses: number,
    income: number,
    date: number[3]
}

export interface AccountDTO {
    id: number,
    amount: number,
    labelAccount: string,
    sheets: Array<SheetDTO>
}

export interface UserAccountDTO {
    id: number,
    amount: number,
    labelAccount: string
}

export interface SheetAverageDTO{
    sheets: SheetDTO[],
    sum: number
}