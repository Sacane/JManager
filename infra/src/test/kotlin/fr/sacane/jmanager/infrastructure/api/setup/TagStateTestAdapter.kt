package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.toDomain
import fr.sacane.jmanager.infrastructure.spi.adapters.toPersonalTag
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.stereotype.Component

data class UserTagsRequest(
    val userId: UserId,
    val tags: List<Tag>
)

//@Component
//class TagStateTestAdapter(
//    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
//    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository,
//    private val userPostgresRepository: UserPostgresRepository
//): State<UserTagsRequest, Tag> {
//    override fun get(): Collection<Tag> {
//        return defaultTagPostgresRepository.findAll().map { it.toDomain() } + tagPersonalPostgresRepository.findAll().map { it.toDomain() }
//    }
//
//    override fun clear() {
//        defaultTagPostgresRepository.deleteAll()
//        tagPersonalPostgresRepository.deleteAll()
//    }
//
//    override fun init(initialState: Collection<UserTagsRequest>) {
//        initialState.forEach { request ->
//            val user = userPostgresRepository.findByIdWithTags(request.userId.value)
//            tagPersonalPostgresRepository.saveAll(
//                request.tags.map { it.toPersonalTag(user) }
//            )
//        }
//    }
//}