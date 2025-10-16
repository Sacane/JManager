package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import java.time.Month

@Port(Side.INFRASTRUCTURE)
interface RegularChecker {
    fun check(bookletId: Long, year: Int, month: Month): Boolean

    fun markAsVerified(bookletId: Long, year: Int, month: Month)
}