package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.infrastructure.JmanagerBackApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(JmanagerBackApplication::class.java)
	}

}
