package fr.sacane.jmanager.app

import fr.sacane.jmanager.JmanagerBackApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(JmanagerBackApplication::class.java)
	}

}
