package fr.sacane.jmanager.infrastructure.rest

import fr.sacane.jmanager.infrastructure.JmanagerApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(JmanagerApplication::class.java)
	}

}
