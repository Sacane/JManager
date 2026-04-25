package fr.sacane.jmanager.domain.port.input.user

data class LoginCommand(val pseudonym: String, val userPassword: String)
