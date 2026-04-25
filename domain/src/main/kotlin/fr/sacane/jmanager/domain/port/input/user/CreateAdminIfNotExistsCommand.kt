package fr.sacane.jmanager.domain.port.input.user

data class CreateAdminIfNotExistsCommand(val username: String, val password: String)
