export default function useTag() {
  const { post, get, deleteQuery } = useQuery()
  const { user } = useAuth()
  async function addPersonalTag(label: string, colorDTO: ColorDTO): Promise<TagDTO> {
    return post('tag', {
      userId: user.value?.id,
      tagLabel: label,
      colorDTO,
    })
  }

  async function getAllTags(): Promise<TagDTO[]> {
    return get(`tag/user/${user.value?.id}`)
  }
  async function deleteTag(id: number): Promise<void> {
    return deleteQuery(`tag/${id}/user/${user.value?.id}`, {})
      .catch(err => console.error(err))
  }
  return { addPersonalTag, getAllTags, deleteTag }
}
