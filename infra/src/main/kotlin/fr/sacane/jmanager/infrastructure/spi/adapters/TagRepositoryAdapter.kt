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
        user.addTag(tag.toEntity())
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

}