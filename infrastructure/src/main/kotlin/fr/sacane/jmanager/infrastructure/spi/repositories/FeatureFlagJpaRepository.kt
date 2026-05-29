package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FeatureFlagJpaRepository : JpaRepository<FeatureFlagEntity, String>
