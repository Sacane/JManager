package fr.sacane.jmanager.application


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync


@SpringBootApplication
@EnableAsync
class JmanagerApplication

fun main(args: Array<String>) {
	runApplication<JmanagerApplication>(*args)
}
