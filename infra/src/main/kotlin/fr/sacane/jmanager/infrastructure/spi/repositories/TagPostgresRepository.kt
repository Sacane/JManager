package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.spi.entity.TagResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TagPostgresRepository : CrudRepository<TagResource, Long>{
    fun existsTagByName(name: String): Boolean

    @Query("SELECT tag FROM TagResource tag WHERE tag.owner.idUser = :userId")
    fun findAllByOwnerId(userId: Long): List<TagResource>
}