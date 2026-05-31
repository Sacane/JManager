package fr.sacane.jmanager.domain.port.output.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.EmailVerificationToken
import fr.sacane.jmanager.domain.models.UserId

@Port(Side.INFRASTRUCTURE)
interface EmailVerificationTokenRepository {
    fun save(token: EmailVerificationToken): EmailVerificationToken
    fun findByToken(token: String): EmailVerificationToken?
    fun deleteByUserId(userId: UserId)
}
