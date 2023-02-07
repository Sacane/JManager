package fr.sacane.jmanager.server.repositories

import fr.sacane.jmanager.server.entity.CategoryResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface CategoryRepository: JpaRepository<CategoryResource, Long>{
    @Transactional
    fun deleteByLabel(label: String)
}