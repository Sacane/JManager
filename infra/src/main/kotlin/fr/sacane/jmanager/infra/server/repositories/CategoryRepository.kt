package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.CategoryResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface CategoryRepository: JpaRepository<CategoryResource, Long>{
    @Transactional
    fun deleteByLabel(label: String)
}