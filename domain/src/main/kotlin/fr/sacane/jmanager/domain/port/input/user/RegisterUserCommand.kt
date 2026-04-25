package fr.sacane.jmanager.domain.port.input.user

data class RegisterUserCommand(val username: String, val password: String, val confirmPassword: String)
