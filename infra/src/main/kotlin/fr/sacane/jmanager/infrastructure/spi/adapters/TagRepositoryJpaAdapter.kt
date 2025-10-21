package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toDomain
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toPersonalTag
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class TagRepositoryJpaAdapter(
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository,
    private val userPostgresRepository: UserPostgresRepository
): TagRepository {
    @Transactional
    override fun save(userId: UserId, tag: Tag): Tag? {
        val user = userPostgresRepository.findByIdWithTags(userId.value) ?: return null
        val tag1 = tag.toPersonalTag()
        val saved = tagPersonalPostgresRepository.save(tag1)
        user.addTag(tag1)
        return saved.toDomain()
    }
    @Transactional
    override fun getAll(userId: UserId)
    : List<Tag> {
        if(userId.value == null) return emptyList()
        return userId.value?.let { id ->
            tagPersonalPostgresRepository.findAllByOwnerId(id)
                .map { it.toDomain() }.plus(
                    defaultTagPostgresRepository.findAll().map { it.toDomain() }
                )
        } ?: emptyList()
    }
    @Transactional
    override fun deleteByLabel(label: String) {
        defaultTagPostgresRepository.deleteByName(label)
    }
    @Transactional
    override fun getAllDefault(userId: UserId): List<Tag> {
        val defaults = defaultTagPostgresRepository.findAll()
        val personal = userId.value?.let { tagPersonalPostgresRepository.findAllByOwnerId(it) } ?: emptyList()
        return defaults.map { it.toDomain() }.plus(personal.map { it.toDomain() })
    }
    @Transactional
    override fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean
    = userId.value?.let { tagPersonalPostgresRepository.existsTagByNameAndOwnerId(tag.label, it) } ?: false

    @Transactional
    override fun saveAll(defaultTags: List<Tag>) {
        defaultTagPostgresRepository.saveAll(defaultTags.map { it.asResource() as DefaultTagResource })
    }
    @Transactional
    override fun existsDefault(): Boolean {
        return defaultTagPostgresRepository.findAll().count() > 2
    }
    @Transactional
    override fun deleteById(tagId: Long): Boolean {
        return tagPersonalPostgresRepository.existsById(tagId).also {
            if(it) tagPersonalPostgresRepository.deleteById(tagId)
        }
    }

    override fun defaultTag(): Tag {
        return defaultTagPostgresRepository.findByName(Tag.noneTag().label)?.toDomain() ?: error("No default tag found")
    }

    @Transactional
    override fun patch(tag: Tag): Tag? {
        try {
            tag.id?.let { tagPersonalPostgresRepository.patchTag(it, tag.label, tag.color.red, tag.color.green, tag.color.blue) } ?: return null
            return tag
        } catch (e: Exception) {
            return null
        }
    }

    override fun existsById(tagId: Long): Boolean {
        return tagPersonalPostgresRepository.existsById(tagId)
    }

    override fun existsAnotherTagByLabel(userId: UserId, tag: Tag): Boolean {
        val tagInBase = userId.value?.let {
            tagPersonalPostgresRepository.findByNameAndOwnerId(tag.label,
                it
            )
        }
        return tagInBase != null && tagInBase.idTag != tag.id
    }
}
