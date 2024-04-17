package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TagResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TagPostgresRepository : CrudRepository<TagResource, Long>{
    fun existsTagByName(name: String): Boolean

    @Query("SELECT tag FROM TagResource tag WHERE tag.owner.idUser = :userId")
    fun findAllByOwnerId(userId: Long): List<TagResource>
    fun deleteByName(name: String)

    @Query("SELECT tag FROM TagResource tag WHERE tag.isDefault = true")
    fun findAllDefault(): List<TagResource>

    @Query("SELECT tag FROM TagResource tag WHERE tag.name = 'Aucune'")
    fun findUnknownTag(): TagResource?
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM TagResource t WHERE t.name = :name AND t.owner.idUser = :ownerId")
    fun existsTagByNameAndOwnerId(name: String, ownerId: Long): Boolean
}
