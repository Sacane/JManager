export default function useTag() {
  const { post, get } = useQuery()
  const { user } = useAuth()
  async function addPersonalTag(label: string, colorDTO: ColorDTO): Promise<TagDTO> {
    return post('tag', {
      userId: user.value?.id,
      tagLabel: label,
      color: colorDTO,
    })
  }

  async function getAllTags(): Promise<TagDTO[]> {
    return get(`tag/user/${user.value?.id}`)
  }
  return { addPersonalTag, getAllTags }
}
