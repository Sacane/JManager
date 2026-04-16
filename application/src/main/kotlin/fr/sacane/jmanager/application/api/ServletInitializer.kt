package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.JmanagerApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(JmanagerApplication::class.java)
	}

}
