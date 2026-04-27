package fr.sacane.jmanager.application.api.session

import fr.sacane.jmanager.domain.port.output.InMemorySessionManager
import fr.sacane.jmanager.domain.port.output.SessionManager
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableAsync
class SessionService(private val session: SessionManager) {
    @Scheduled(fixedDelay = InMemorySessionManager.PURGE_DELAY)
    fun launchPeriodic() {
        session.purgeExpiredToken()
    }
}