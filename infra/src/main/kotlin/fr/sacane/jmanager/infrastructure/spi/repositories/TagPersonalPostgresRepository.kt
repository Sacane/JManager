package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TagPersonalPostgresRepository: CrudRepository<TagPersonalResource, Long> {
    fun deleteByName(name: String)
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM TagPersonalResource t WHERE t.name = :name AND t.owner.idUser = :ownerId")
    fun existsTagByNameAndOwnerId(name: String, ownerId: Long): Boolean
    @Query("SELECT tag FROM TagPersonalResource tag WHERE tag.owner.idUser = :userId")
    fun findAllByOwnerId(userId: Long): List<TagPersonalResource>

    @Query("SELECT tag FROM TagPersonalResource tag WHERE tag.idTag = :id")
    fun findByIdNullable(id: Long?): TagPersonalResource?
}