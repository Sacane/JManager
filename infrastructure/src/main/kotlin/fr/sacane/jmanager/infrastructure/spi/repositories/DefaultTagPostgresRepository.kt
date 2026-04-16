package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
interface DefaultTagPostgresRepository : CrudRepository<DefaultTagResource, UUID>{
    fun existsTagByName(name: String): Boolean
    @Modifying
    @Transactional
    fun deleteByName(name: String)
    @Query("SELECT tag FROM DefaultTagResource tag WHERE tag.name = 'Aucune'")
    fun findUnknownTag(): DefaultTagResource?

    @Query("SELECT tag FROM DefaultTagResource tag WHERE tag.idTag = :id")
    fun findByIdNullable(id: UUID): DefaultTagResource?

    fun findByName(name: String): DefaultTagResource?
}
