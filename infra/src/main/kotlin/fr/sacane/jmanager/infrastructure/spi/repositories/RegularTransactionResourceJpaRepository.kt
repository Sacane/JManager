package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface RegularTransactionResourceJpaRepository: JpaRepository<RegularTransactionEntity, UUID> {

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.accounts")
	fun findAllWithAccounts(): List<RegularTransactionEntity>

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.accounts WHERE rt.owner.idUser = :userId")
	fun findAllByOwnerIdWithAccounts(userId: UUID): List<RegularTransactionEntity>

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.accounts WHERE rt.transactionId = :id")
	fun findByIdWithAccounts(id: UUID): RegularTransactionEntity?
}
