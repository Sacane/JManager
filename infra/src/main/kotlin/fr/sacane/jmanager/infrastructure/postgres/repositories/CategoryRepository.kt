package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.CategoryResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface CategoryRepository: CrudRepository<CategoryResource, Long> {
    @Transactional
    fun deleteByLabel(label: String)
}