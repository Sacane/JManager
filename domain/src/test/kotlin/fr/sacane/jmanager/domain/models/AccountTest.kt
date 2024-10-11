package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.port.spi.mock.Directory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month

class AccountTest {

    @Test
    fun `the user's accounts should not contains the same value more than once`(){
        val constantValue = 102.toAmount()
        val accounts = mutableListOf(
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Courant", mutableListOf()),
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Secondaire", mutableListOf())
        )

        val pwdUser = Password("D5301012000MAMacita")
        val user = User(UserId(1), "johan", "johan.test@test.fr", accounts)
        assertTrue{
            user.accounts.contains(Account(null, constantValue, "test", mutableListOf()))
        }
    }

    @Test
    fun `by giving a year and a month, accounts should retrieve its corresponding sheets`(){
        val sheets = Directory.transactionInventories
        val account = Account(2.toLong(), 1050.toAmount(), "Primary", sheets)
        val sheetsOfDecember = account.retrieveSheetSurroundAndSortedByDate(Month.DECEMBER, 2022)
        assertTrue {
            sheetsOfDecember != null && sheetsOfDecember.all { it.date.month == Month.DECEMBER && it.date.year == 2022 } && sheetsOfDecember.size == 3
        }
    }
}
