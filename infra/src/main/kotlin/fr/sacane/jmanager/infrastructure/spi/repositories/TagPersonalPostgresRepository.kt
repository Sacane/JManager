package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface TagPersonalPostgresRepository: CrudRepository<TagPersonalResource, UUID> {
    fun deleteByName(name: String)
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM TagPersonalResource t WHERE t.name = :name AND t.owner.idUser = :ownerId")
    fun existsTagByNameAndOwnerId(name: String, ownerId: UUID): Boolean
    @Query("SELECT tag FROM TagPersonalResource tag WHERE tag.owner.idUser = :userId")
    fun findAllByOwnerId(userId: UUID): List<TagPersonalResource>

    @Query("SELECT tag FROM TagPersonalResource tag WHERE tag.idTag = :id")
    fun findByIdNullable(id: UUID?): TagPersonalResource?

    @Modifying
    @Query("UPDATE TagPersonalResource tag SET tag.name = :name, tag.color.blue = :blue, tag.color.red = :red, tag.color.green = :green WHERE tag.idTag = :id")
    fun patchTag(id: UUID, name: String, red: Int, green: Int, blue: Int)

    @Query("SELECT tag FROM TagPersonalResource tag WHERE tag.name = :name AND tag.owner.idUser = :ownerId")
    fun findByNameAndOwnerId(name: String, ownerId: UUID): TagPersonalResource?
}