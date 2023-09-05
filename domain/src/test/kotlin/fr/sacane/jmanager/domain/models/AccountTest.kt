package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.port.spi.mock.Directory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Month

class AccountTest {

    @Test
    fun `account should be capable to earn and loss amount correctly`(){
        val account = Account(null, 100.toDouble(), "courant", mutableListOf())
        account += 20.toDouble()
        account.loss(10.toDouble())
        account.earn(50.toDouble())
        account -= 15.toDouble()
        assertEquals(account.sold, 145.toDouble())
    }

    @Test
    fun `transaction should give and take amount between two account transaction`(){
        val account = Account(null, 100.toDouble(), "courant", mutableListOf())
        val account2 = Account(null, 100.toDouble(), "secondaire", mutableListOf())
        account.transaction(10.toDouble(), account2, true)
        assertEquals(account.sold, 110.toDouble())
        assertEquals(account2.sold, 90.toDouble())
    }

    @Test
    fun `the user's accounts should not contains the same value more than once`(){
        val constantValue = 102.toDouble()
        val accounts = mutableListOf(
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Courant", mutableListOf()),
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Secondaire", mutableListOf())
        )

        val pwdUser = Password("D5301012000MAMacita")
        val user = User(UserId(1), "johan", "johan.test@test.fr", accounts, pwdUser, CategoryFactory.allDefaultCategories())
        assertTrue{
            user.accounts.contains(Account(null, constantValue, "test", mutableListOf()))
        }
    }

    @Test
    fun `by giving a year and a month, accounts should retrieve its corresponding sheets`(){
        val sheets = Directory.sheetInventory
        val account = Account(2.toLong(), 1050.toDouble(), "Primary", sheets)
        val sheetsOfDecember = account.retrieveSheetSurroundByDate(Month.DECEMBER, 2022)
        assertTrue {
            sheetsOfDecember != null && sheetsOfDecember.all { it.date.month == Month.DECEMBER && it.date.year == 2022 } && sheetsOfDecember.size == 3
        }
    }
}
