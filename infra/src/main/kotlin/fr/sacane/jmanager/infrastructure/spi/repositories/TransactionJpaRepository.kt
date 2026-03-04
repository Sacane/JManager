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
}
