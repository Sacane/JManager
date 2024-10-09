export default function usePreviewTransaction() {
  const { post, get } = useQuery()
  const { user } = useAuth()

  function savePreviewTransaction(accountId: number, sheetDTO: SheetDTO): Promise<SheetDTO> {
    return post('sheet', {
      userId: user.value?.id,
      accountLabel,
      sheetDTO,
    })
  }

  return { savePreviewTransaction }
}
