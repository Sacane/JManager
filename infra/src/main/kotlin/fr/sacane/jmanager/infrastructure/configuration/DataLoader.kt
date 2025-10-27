package fr.sacane.jmanager.infrastructure.configuration

import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.port.api.UserFeature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class DataLoader (
    private val tagFeature: TagFeature,
    private val userFeature: UserFeature,
    @param:Value("\${jmanager.admin.username}") private val adminUsername: String,
    @param:Value("\${jmanager.admin.password}") private val adminPassword: String,
): ApplicationListener<ContextRefreshedEvent> {
    private var isSetup = false
    private val log = LoggerFactory.getLogger(DataLoader::class.java)
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if(isSetup){
            return
        }
        tagFeature.addDefaultTags()
        val adminCreationResult = userFeature.createAdminIfNotExists(adminUsername, adminPassword)
        check(!adminCreationResult.isFailure()){
            "The admin user could not be created"
        }
        isSetup = true
        log.info("The tags has been loaded successfully")
    }
}