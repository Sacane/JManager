package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularTransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface MonthlyTransactionResourceJpaRepository: JpaRepository<MonthlyRegularTransactionEntity, UUID>