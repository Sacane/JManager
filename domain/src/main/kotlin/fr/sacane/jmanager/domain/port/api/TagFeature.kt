package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

@Port(Side.APPLICATION)
sealed interface TagFeature {
    fun addTag(token: String, tag: Tag): Result<Tag>
    fun getAllTags(token: String): Result<List<Tag>>
    fun addDefaultTags()
    fun deleteTag(token: String, tagId: UUID): Result<Nothing>
    fun defaultTag(token: String): Result<Tag>
    fun editTag(token: String, tag: Tag): Result<Tag>
}

@DomainService
class TagFeatureImpl(
    private val tagRepository: TagRepository,
    private val session: SessionManager
): TagFeature {
    override fun addTag(token: String, tag: Tag): Result<Tag> = session.authenticate(token){
        if(tag.isDefault || tagRepository.existsByLabelAndUserId(it, tag)) {
            return@authenticate failure(ResultState.TAG_LABEL_ALREADY_TAKEN, "Label '${tag.label}' is already taken by the user ${it.value}")
        }
        val save = tagRepository.save(it, tag) ?: return@authenticate notFound("User has not been found")
        success(save)
    }

    override fun getAllTags(token: String): Result<List<Tag>> = session.authenticate(token) {
        success(tagRepository.getAllDefault(it))
    }

    override fun addDefaultTags() {
        if(tagRepository.existsDefault()){
            return
        }
        tagRepository.saveAll(defaultTags)
    }

    override fun deleteTag(token: String, tagId: UUID): Result<Nothing> = session.authenticate(token){
        if(!tagRepository.deleteById(tagId)) {
            return@authenticate notFound("Tag with id $tagId has not been found")
        }
        success()
    }

    override fun defaultTag(token: String): Result<Tag> = session.authenticate(token) {
        val tagResult = tagRepository.defaultTag() ?: return@authenticate notFound("Il n'y a pas de tag par défaut d'enregistré")
        return@authenticate success(tagResult)
    }

    override fun editTag(token: String, tag: Tag): Result<Tag> = session.authenticate(token) {
        if(tag.id == null || !tagRepository.existsById(tag.id)) {
            return@authenticate notFound("Tag with id ${tag.id} has not been found")
        }
        if(
            tagRepository.existsAnotherTagByLabel(it, tag)
        ) {
            return@authenticate failure(ResultState.TAG_LABEL_ALREADY_TAKEN, "Label '${tag.label}' is already taken")
        }
        if(tag.isDefault) {
            return@authenticate failure(ResultState.TAG_SHOULD_NOT_BE_DEFAULT, "Tag should not be default")
        }
        val save = tagRepository.patch(tag) ?: return@authenticate notFound("User has not been found")
        success(save)
    }
}