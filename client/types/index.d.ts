

export interface SheetDTO{
    id: number,
    label: string,
    expenses: string,
    income: string,
    date: string,
    accountAmount: string,
    
}

export interface AccountDTO {
    id: number | undefined,
    amount: string,
    labelAccount: string,
    sheets: Array<SheetDTO>
}

export interface UserAccountDTO {
    id: number,
    amount: string,
    labelAccount: string
}

export interface SheetAverageDTO{
    sheets: SheetDTO[],
    sum: number
}