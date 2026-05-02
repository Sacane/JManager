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

data class DefaultTagQuery(
    val userId: UserId
) : Query<Tag>

@Port(Side.APPLICATION)
interface DefaultTagUseCase : QueryHandler<DefaultTagQuery, Tag> {
    override val queryClass get() = DefaultTagQuery::class
}

@DomainService
class DefaultTagService(
    private val tagRepository: TagRepository
) : DefaultTagUseCase {

    override fun handle(query: DefaultTagQuery): Result<Tag> {
        val tagResult = tagRepository.defaultTag()
        return success(tagResult)
    }
}
