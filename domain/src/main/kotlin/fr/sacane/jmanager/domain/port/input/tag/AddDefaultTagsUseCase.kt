package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.output.repository.TagRepository

@Port(Side.APPLICATION)
interface AddDefaultTagsUseCase {
    fun handle()
}

@DomainService
class AddDefaultTagsService(
    private val tagRepository: TagRepository
) : AddDefaultTagsUseCase {

    override fun handle() {
        if (tagRepository.existsDefault()) {
            return
        }
        tagRepository.saveAll(defaultTags)
    }
}
