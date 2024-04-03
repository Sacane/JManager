package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface UserPostgresRepository: CrudRepository<UserResource, Long> {
    fun findByUsername(username: String): UserResource?

    @Transactional
    fun deleteByUsername(username: String)

    @Query("SELECT user FROM UserResource user LEFT JOIN FETCH user.accounts WHERE user.idUser = :id")
    fun findByIdWithAccount(id: Long): UserResource?

    @Query("SELECT user from UserResource user LEFT JOIN FETCH user.accounts")
    fun findByIdWithSheets(id: Long): UserResource?
}
