package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.port.api.SessionFeature
import org.springframework.stereotype.Component

@Component
class SessionRepositoryStateAdapter(
    private val sessionFeature: SessionFeature
) {

}