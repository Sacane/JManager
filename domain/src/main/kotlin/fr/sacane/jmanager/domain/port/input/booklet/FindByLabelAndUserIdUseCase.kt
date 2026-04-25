package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface FindByLabelAndUserIdUseCase {
    fun findByLabelAndUserId(token: SessionToken, label: String): Result<Booklet>
}
