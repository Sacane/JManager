package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toDomain
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toPersonalTag
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TagRepositoryJpaAdapter(
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository,
    private val userPostgresRepository: UserPostgresRepository
): TagRepository {
    @CacheEvict(cacheNames = ["allTags"], allEntries = true)
    @Transactional
    override fun save(userId: UserId, tag: Tag): Tag? {
        val id = userId.value ?: return null
        val user = userPostgresRepository.findByIdWithTags(id) ?: return null
        val tag1 = tag.toPersonalTag()
        val saved = tagPersonalPostgresRepository.save(tag1)
        user.addTag(saved)
        return saved.toDomain()
    }
    @Transactional
    override fun getAll(userId: UserId)
    : List<Tag> {
        val id = userId.value ?: return emptyList()
        return tagPersonalPostgresRepository.findAllByOwnerId(id)
            .map { it.toDomain() }.plus(defaultTagPostgresRepository.findAll().map { it.toDomain() })
    }
    @Transactional
    override fun deleteByLabel(label: String) {
        defaultTagPostgresRepository.deleteByName(label)
    }
    @Cacheable(cacheNames = ["allTags"], key = "#userId")
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
    @CacheEvict(cacheNames = ["allTags"], allEntries = true)
    @Transactional
    override fun deleteById(tagId: UUID): Boolean {
        return tagPersonalPostgresRepository.existsById(tagId).also {
            if(it) tagPersonalPostgresRepository.deleteById(tagId)
        }
    }

    @Cacheable(cacheNames = ["defaultTag"])
    override fun defaultTag(): Tag {
        val found = defaultTagPostgresRepository.findAll().firstOrNull { it.name == Tag.noneTag().label }
        return found?.toDomain() ?: error("No default tag found")
    }

    @CacheEvict(cacheNames = ["allTags"], allEntries = true)
    @Transactional
    override fun patch(tag: Tag): Tag? {
        try {
            tag.id?.let { tagPersonalPostgresRepository.patchTag(it, tag.label, tag.color.red, tag.color.green, tag.color.blue) } ?: return null
            return tag
        } catch (_: Exception) {
            return null
        }
    }

    override fun existsById(tagId: UUID): Boolean {
        return tagPersonalPostgresRepository.existsById(tagId)
    }

    override fun existsAnotherTagByLabel(userId: UserId, tag: Tag): Boolean {
        val id = userId.value ?: return false
        val tagInBase = tagPersonalPostgresRepository.findByNameAndOwnerId(tag.label, id)
        return tagInBase != null && tagInBase.idTag != tag.id
    }
}
