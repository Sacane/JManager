package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TagRepository

data class UserTag(
    val userId: UserId,
    val tags: MutableList<Tag>
)
class InMemoryTagRepository(
    private val inMemoryDatabase: InMemoryDatabase
): TagRepository, BiState<UserTag, List<Tag>> {
    override fun save(userId: UserId, tag: Tag): Tag? {
        inMemoryDatabase.userByTag.computeIfAbsent(userId) { mutableListOf() }.add(tag)
        return tag
    }

    override fun getAll(userId: UserId): List<Tag> {
        return inMemoryDatabase.userByTag[userId] ?: emptyList()
    }

    override fun deleteByLabel(label: String) {
        inMemoryDatabase.userByTag.forEach { (_, tags) -> tags.removeIf { it.label == label } }
    }

    override fun getAllDefault(userId: UserId): List<Tag> {
        return inMemoryDatabase.userByTag[userId]?.plus(inMemoryDatabase.defaultTags) ?: emptyList()
    }

    override fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean {
        return inMemoryDatabase.userByTag[userId]?.any { it.label == tag.label } ?: false
    }

    override fun saveAll(defaultTags: List<Tag>) {
    }

    override fun existsDefault(): Boolean {
        return inMemoryDatabase.defaultTags.isNotEmpty()
    }

    override fun deleteById(tagId: Long): Boolean {
        var result = false
        inMemoryDatabase.userByTag.forEach { (_, tags) -> result = tags.removeIf { it.id == tagId } }
        return result
    }

    override fun defaultTag(): Tag {
        return inMemoryDatabase.defaultTags.find { it.label == "Aucune" }!!
    }

    override fun patch(tag: Tag): Tag? {
        inMemoryDatabase.userByTag.forEach { (_, tags) ->
            tags.removeIf { it.id == tag.id }
            tags.add(tag)
        }
        return tag
    }

    override fun existsAnotherTagByLabel(userId: UserId, tag: Tag): Boolean {
        return inMemoryDatabase.userByTag[userId]?.any { it.label == tag.label && it.id != tag.id } ?: false
    }

    override fun existsById(tagId: Long): Boolean {
        return inMemoryDatabase.userByTag.values.flatten().any { it.id == tagId }
    }

    override fun getStates(): List<Tag> {
        return inMemoryDatabase.defaultTags + inMemoryDatabase.userByTag.values.flatten()
    }

    override fun clear() {
        inMemoryDatabase.userByTag.clear()
    }

    override fun init(initialState: UserTag) {
        inMemoryDatabase.userByTag[initialState.userId] = initialState.tags
    }
}