package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
interface UserPostgresRepository: CrudRepository<UserResource, UUID> {
    fun findByUsername(username: String): UserResource?

    @Transactional
    fun deleteByUsername(username: String)

    @Query("SELECT user FROM UserResource user LEFT JOIN FETCH user.booklets WHERE user.idUser = :id")
    fun findByIdWithBooklets(id: UUID): UserResource?

    @Query("SELECT user FROM UserResource user LEFT JOIN FETCH user.tags WHERE user.idUser = :id")
    fun findByIdWithTags(id: UUID?): UserResource?

    @Modifying
    @Transactional
    @Query("UPDATE UserResource user SET user.projectionWindowDays = :projectionWindowDays WHERE user.idUser = :id")
    fun updateProjectionWindowDays(@Param("id") id: UUID, @Param("projectionWindowDays") projectionWindowDays: Int): Int
}