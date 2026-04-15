package fr.sacane.jmanager.application


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class JmanagerApplication

fun main(args: Array<String>) {
	runApplication<JmanagerApplication>(*args)
}
