package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface RegularTransactionResourceJpaRepository: JpaRepository<RegularTransactionEntity, UUID> {

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.booklets")
	fun findAllWithBooklets(): List<RegularTransactionEntity>

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.booklets WHERE rt.owner.idUser = :userId")
	fun findAllByOwnerIdWithBooklets(userId: UUID): List<RegularTransactionEntity>

	@Query("SELECT DISTINCT rt FROM RegularTransactionEntity rt LEFT JOIN FETCH rt.booklets WHERE rt.transactionId = :id")
	fun findByIdWithBooklets(id: UUID): RegularTransactionEntity?

	@Query(
		value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM regular_transaction WHERE personal_tag_id = :tagId",
		nativeQuery = true
	)
	fun existsByPersonalTagId(@Param("tagId") tagId: UUID): Boolean

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		value = "UPDATE regular_transaction SET personal_tag_id = NULL, tag_id = :defaultTagId WHERE personal_tag_id = :tagId",
		nativeQuery = true
	)
	fun replacePersonalTagByDefaultId(@Param("tagId") tagId: UUID, @Param("defaultTagId") defaultTagId: UUID)
}
