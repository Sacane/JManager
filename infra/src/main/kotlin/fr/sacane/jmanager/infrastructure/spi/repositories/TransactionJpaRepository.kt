package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionJpaRepository : CrudRepository<TransactionResource, UUID> {
    fun findSheetResourceByIdSheet(id: UUID): TransactionResource?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TransactionResource t WHERE t.idSheet IN :sheetIds")
    fun deleteAllByIdSheetIn(@Param("sheetIds") sheetIds: List<UUID>): Int

    @Query(
        value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM sheet WHERE personal_tag_id_tag = :tagId",
        nativeQuery = true
    )
    fun existsByPersonalTagId(@Param("tagId") tagId: UUID): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "UPDATE sheet SET personal_tag_id_tag = NULL, tag_id_tag = :defaultTagId WHERE personal_tag_id_tag = :tagId",
        nativeQuery = true
    )
    fun replacePersonalTagByDefaultId(@Param("tagId") tagId: UUID, @Param("defaultTagId") defaultTagId: UUID)
}
