package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class GetAllTagsQuery(
    val userId: UserId
) : Query<List<Tag>>

@Port(Side.APPLICATION)
interface GetAllTagsUseCase : QueryHandler<GetAllTagsQuery, List<Tag>> {
    override val queryClass get() = GetAllTagsQuery::class
}

@DomainService
class GetAllTagsService(
    private val tagRepository: TagRepository
) : GetAllTagsUseCase {

    override fun handle(query: GetAllTagsQuery): Result<List<Tag>> {
        return success(tagRepository.getAllDefault(query.userId))
    }
}
