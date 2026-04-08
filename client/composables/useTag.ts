export default function useTag() {
  const { post, get, deleteQuery, patch } = useQuery()
  async function addPersonalTag(label: string, colorDTO: ColorDTO): Promise<TagDTO> {
    return post('tag', {
      tagLabel: label,
      colorDTO,
    })
  }

  async function getAllTags(): Promise<TagDTO[]> {
    return get(`tag`)
  }
  async function deleteTag(id: string, force: boolean = false): Promise<void> {
    return deleteQuery(`tag/${id}${force ? '?force=true' : ''}`, {})
  }
  async function getDefaultTag(): Promise<TagDTO> {
    return get(`tag/default`)
  }
  async function editTag(tag: TagDTO): Promise<TagDTO> {
    return patch('tag', tag)
  }
  return { addPersonalTag, getAllTags, deleteTag, getDefaultTag, editTag }
}
