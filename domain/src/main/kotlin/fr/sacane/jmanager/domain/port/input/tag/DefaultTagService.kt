package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

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
