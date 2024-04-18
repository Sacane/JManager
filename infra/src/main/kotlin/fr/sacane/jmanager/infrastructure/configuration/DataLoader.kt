package fr.sacane.jmanager.infrastructure.configuration

import fr.sacane.jmanager.domain.port.api.TagFeature
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class DataLoader (
    val tagFeature: TagFeature
): ApplicationListener<ContextRefreshedEvent> {
    private var isSetup = false
    private val log = LoggerFactory.getLogger(DataLoader::class.java)
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if(isSetup){
            return
        }
        tagFeature.addDefaultTags()
        isSetup = true
        log.info("The tags has been loaded successfully")
    }
}