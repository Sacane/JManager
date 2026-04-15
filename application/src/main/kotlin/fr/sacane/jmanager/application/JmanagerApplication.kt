package fr.sacane.jmanager.application


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories


@SpringBootApplication
@EntityScan(basePackages = ["fr.sacane.jmanager.infrastructure.spi.entity"])
@EnableJpaRepositories(basePackages = ["fr.sacane.jmanager.infrastructure.spi"])
class JmanagerApplication

fun main(args: Array<String>) {
	runApplication<JmanagerApplication>(*args)
}
