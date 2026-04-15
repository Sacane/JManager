package fr.sacane.jmanager.application.api.setup

import fr.sacane.jmanager.domain.port.api.UserFeature
import org.springframework.stereotype.Component

@Component
class SessionRepositoryStateAdapter(
    private val userFeature: UserFeature
) {

}