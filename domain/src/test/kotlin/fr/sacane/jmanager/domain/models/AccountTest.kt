package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.port.spi.mock.Directory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month

class AccountTest {

    @Test
    fun `account should be capable to earn and loss amount correctly`(){
        val account = Account(null, Amount(BigDecimal(100)), "courant")
        account += 20.toAmount()
        println(account.sold)
        account -= 10.toAmount()
        account += 50.toAmount()
        println(account.sold)
        account -= 15.toAmount()
        assertEquals(account.sold, 145.toAmount())
    }

    @Test
    fun `transaction should give and take amount between two account transaction`(){
        val account = Account(null, 100.toAmount(), "courant")
        val account2 = Account(null, 100.toAmount(), "secondaire")
        account.transaction(10.toAmount(), account2, true)
        assertEquals(account.sold, 110.toAmount())
        assertEquals(account2.sold, 90.toAmount())
    }

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
        val user = User(UserId(1), "johan", "johan.test@test.fr", accounts, pwdUser, tags().toMutableList())
        assertTrue{
            user.accounts.contains(Account(null, constantValue, "test", mutableListOf()))
        }
    }

    @Test
    fun `by giving a year and a month, accounts should retrieve its corresponding sheets`(){
        val sheets = Directory.sheetInventory
        val account = Account(2.toLong(), 1050.toAmount(), "Primary", sheets)
        val sheetsOfDecember = account.retrieveSheetSurroundByDate(Month.DECEMBER, 2022)
        assertTrue {
            sheetsOfDecember != null && sheetsOfDecember.all { it.date.month == Month.DECEMBER && it.date.year == 2022 } && sheetsOfDecember.size == 3
        }
    }
}
