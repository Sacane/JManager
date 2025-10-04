package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.port.spi.RegularChecker
import java.time.Month

class InMemoryRegularChecker: RegularChecker {
    override fun check(bookletId: Long, year: Int, month: Month): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAsVerified(bookletId: Long, year: Int, month: Month) {
        TODO("Not yet implemented")
    }
}