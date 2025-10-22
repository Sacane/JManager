package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.mock.Directory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Month
import java.util.UUID

class AccountTest {

    @Test
    fun `the user's accounts should not contains the same value more than once`(){
        val constantValue = 102.toAmount()
        val booklets = mutableListOf(
            Booklet(constantValue, "test", mutableListOf()),
            Booklet(constantValue, "Courant", mutableListOf()),
            Booklet(constantValue, "test", mutableListOf()),
            Booklet(constantValue, "Secondaire", mutableListOf())
        )

        val user = User(UserId(UUID.randomUUID()), "johan", "johan.test@test.fr", booklets)
        assertTrue{
            user.booklets.contains(Booklet(constantValue, "test", mutableListOf()))
        }
    }

    @Test
    fun `by giving a year and a month, accounts should retrieve its corresponding sheets`(){
        val sheets = Directory.transactionInventories
        val booklet = Booklet(1050.toAmount(), "Primary", sheets, id = UUID.randomUUID())
        val sheetsOfDecember = booklet.retrieveSheetSurroundAndSortedByDate(Month.DECEMBER, 2022)
        assertTrue {
            sheetsOfDecember.all { it.date.month == Month.DECEMBER && it.date.year == 2022 } && sheetsOfDecember.size == 3
        }
    }
}
