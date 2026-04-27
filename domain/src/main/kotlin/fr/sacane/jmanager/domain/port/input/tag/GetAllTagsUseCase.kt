package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class GetAllTagsQuery(
    val token: SessionToken
) : Query<List<Tag>>

@Port(Side.APPLICATION)
interface GetAllTagsUseCase : QueryHandler<GetAllTagsQuery, List<Tag>> {
    override val queryClass get() = GetAllTagsQuery::class
}

@DomainService
class GetAllTagsService(
    private val tagRepository: TagRepository,
    private val session: SessionManager
) : GetAllTagsUseCase {

    override fun handle(query: GetAllTagsQuery): Result<List<Tag>> = session.authenticate(query.token) {
        success(tagRepository.getAllDefault(it))
    }
}
