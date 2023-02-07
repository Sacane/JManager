package fr.sacane.jmanager.app

import fr.sacane.jmanager.config.JmanagerConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(JmanagerConfiguration::class)
@ComponentScan(basePackages = ["fr.sacane.jmanager.server", "fr.sacane.jmanager.config"])
class JmanagerBackApplication

fun main(args: Array<String>) {
	runApplication<JmanagerBackApplication>(*args)
}
