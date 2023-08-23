

export interface SheetDTO{
    id: number,
    label: string,
    amount: number,
    action: boolean,
    date: Date
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

