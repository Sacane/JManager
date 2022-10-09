package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.CategoryResource
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository: JpaRepository<CategoryResource, Long>