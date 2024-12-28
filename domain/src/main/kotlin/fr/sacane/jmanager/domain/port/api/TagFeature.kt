package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.*

@Port(Side.APPLICATION)
sealed interface TagFeature {
    fun addTag(userId: UserId, token: UUID, tag: Tag): Result<Tag>
    fun getAllTags(userId: UserId, token: UUID): Result<List<Tag>>
    fun addDefaultTags()
    fun deleteTag(userId: UserId, token: UUID, tagId: Long): Result<Nothing>
    fun defaultTag(userId: UserId, token: UUID): Result<Tag>
}

@DomainService
class TagFeatureImpl(
    private val tagRepository: TagRepository,
    private val session: InMemorySessionManager
): TagFeature {
    override fun addTag(userId: UserId, token: UUID, tag: Tag): Result<Tag> = session.authenticate(userId, token){
        if(tag.isDefault || tagRepository.existsByLabelAndUserId(it, tag)) {
            return@authenticate failure(ResultState.TAG_LABEL_ALREADY_TAKEN, "Label '${tag.label}' is already taken by the user ${it.value}")
        }
        val save = tagRepository.save(it, tag) ?: return@authenticate notFound("User has not been found")
        success(save)
    }

    override fun getAllTags(userId: UserId, token: UUID): Result<List<Tag>> = session.authenticate(userId, token) {
        success(tagRepository.getAllDefault(userId))
    }

    override fun addDefaultTags() {
        if(tagRepository.existsDefault()){
            return
        }
        tagRepository.saveAll(defaultTags)
    }

    override fun deleteTag(userId: UserId, token: UUID, tagId: Long): Result<Nothing> = session.authenticate(userId, token){
        if(!tagRepository.deleteById(tagId)) {
            return@authenticate notFound("Tag with id $tagId has not been found")
        }
        success()
    }

    override fun defaultTag(userId: UserId, token: UUID): Result<Tag> = session.authenticate(userId, token) {
        val tagResult = tagRepository.defaultTag() ?: return@authenticate notFound("Il n'y a pas de tag par défaut d'enregistré")
        return@authenticate success(tagResult)
    }
}