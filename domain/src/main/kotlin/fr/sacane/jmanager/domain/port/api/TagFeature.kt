package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import java.util.*

@Port(Side.APPLICATION)
sealed interface TagFeature {
    fun addTag(userId: UserId, token: UUID, tag: Tag): Response<Tag>
    fun getAllTags(userId: UserId, token: UUID): Response<List<Tag>>
    fun addDefaultTags()
    fun deleteTag(userId: UserId, token: UUID, tagId: Long): Response<Nothing>
}

@DomainService
class TagFeatureImpl(
    private val register: TransactionRegister,
    private val tagRepository: TagRepository,
    private val session: SessionManager
): TagFeature {
    override fun addTag(userId: UserId, token: UUID, tag: Tag): Response<Tag> = session.authenticate(userId, token){
        if(tag.isDefault || tagRepository.existsByLabelAndUserId(it, tag)) {
            return@authenticate Response.invalid("Le label '${tag.label}' est déjà pris")
        }
        val save = tagRepository.save(it, tag) ?: return@authenticate Response.notFound("User has not been found")
        Response.ok(save)
    }

    override fun getAllTags(userId: UserId, token: UUID): Response<List<Tag>> = session.authenticate(userId, token) {
        Response.ok(tagRepository.getAllDefault(userId))
    }

    override fun addDefaultTags() {
        if(tagRepository.existsDefault()){
            return
        }
        tagRepository.saveAll(defaultTags)
    }

    override fun deleteTag(userId: UserId, token: UUID, tagId: Long): Response<Nothing> = session.authenticate(userId, token){
        tagRepository.deleteById(tagId)
        Response.ok()
    }
}