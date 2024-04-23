package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId

interface TagRepository {
    fun save(userId: UserId, tag: Tag): Tag?
    fun getAll(userId: UserId): List<Tag>
    fun deleteByLabel(label: String)
    fun getAllDefault(userId: UserId): List<Tag>
    fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean
    fun saveAll(defaultTags: List<Tag>)
    fun existsDefault(): Boolean
    fun deleteById(tagId: Long)
}
