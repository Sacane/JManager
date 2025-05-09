export default function useTag() {
  const { post, get, deleteQuery } = useQuery()
  async function addPersonalTag(label: string, colorDTO: ColorDTO): Promise<TagDTO> {
    return post('tag', {
      tagLabel: label,
      colorDTO,
    })
  }

  async function getAllTags(): Promise<TagDTO[]> {
    return get(`tag`)
  }
  async function deleteTag(id: number): Promise<void> {
    return deleteQuery(`tag/${id}`, {})
      .catch(err => console.error(err))
  }
  async function getDefaultTag(): Promise<TagDTO> {
    return get(`tag/default`)
  }
  return { addPersonalTag, getAllTags, deleteTag, getDefaultTag }
}
