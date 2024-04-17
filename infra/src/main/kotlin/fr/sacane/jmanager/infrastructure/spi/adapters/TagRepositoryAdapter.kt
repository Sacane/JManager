package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class TagRepositoryAdapter(
    private val tagPostgresRepository: TagPostgresRepository,
    private val userPostgresRepository: UserPostgresRepository
): TagRepository {
    @Transactional
    override fun save(userId: UserId, tag: Tag): Tag? {
        val user = userPostgresRepository.findByIdWithTags(userId.id) ?: return null
        val tag1 = tag.toEntity()
        user.addTag(tag1)
        return tag
    }
    @Transactional
    override fun getAll(userId: UserId)
    : List<Tag> {
        if(userId.id == null) return emptyList()
        return userId.id?.let {id ->
            tagPostgresRepository.findAllByOwnerId(id)
                .map { it.toModel() }
        } ?: emptyList()
    }
    @Transactional
    override fun deleteByLabel(label: String) {
        tagPostgresRepository.deleteByName(label)
    }
    @Transactional
    override fun getAllDefault(userId: UserId): List<Tag> {
        val defaults = tagPostgresRepository.findAllDefault()
        val personal = userId.id?.let { tagPostgresRepository.findAllByOwnerId(it) } ?: emptyList()
        return defaults.map { it.toModel() }.plus(personal.map { it.toModel() })
    }
    @Transactional
    override fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean
    = userId.id?.let { tagPostgresRepository.existsTagByNameAndOwnerId(tag.label, it) } ?: false

}
