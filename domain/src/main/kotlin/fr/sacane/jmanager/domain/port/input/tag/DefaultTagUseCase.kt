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

data class DefaultTagQuery(
    val token: SessionToken
) : Query<Tag>

@Port(Side.APPLICATION)
interface DefaultTagUseCase : QueryHandler<DefaultTagQuery, Tag> {
    override val queryClass get() = DefaultTagQuery::class
}

@DomainService
class DefaultTagService(
    private val tagRepository: TagRepository,
    private val session: SessionManager
) : DefaultTagUseCase {

    override fun handle(query: DefaultTagQuery): Result<Tag> = session.authenticate(query.token) {
        val tagResult = tagRepository.defaultTag()
        return@authenticate success(tagResult)
    }
}
