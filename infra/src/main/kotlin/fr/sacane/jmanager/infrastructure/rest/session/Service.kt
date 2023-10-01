package fr.sacane.jmanager.infrastructure.rest.session

import fr.sacane.jmanager.domain.port.api.SessionManager
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableAsync
class SessionService(private val session: SessionManager) {
    @Scheduled(fixedDelay = SessionManager.PURGE_DELAY)
    fun launchPeriodic() {
        session.purgeExpiredToken()
    }
}