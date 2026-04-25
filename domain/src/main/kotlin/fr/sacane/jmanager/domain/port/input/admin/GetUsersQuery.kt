package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.models.SessionToken

data class GetUsersQuery(
    val token: SessionToken,
    val pageNumber: Int = 0,
    val pageSize: Int = 20
)
