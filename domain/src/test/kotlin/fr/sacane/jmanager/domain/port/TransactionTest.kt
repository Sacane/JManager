package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import fr.sacane.jmanager.domain.port.spi.mock.Directory
import org.junit.jupiter.api.Test

class TransactionTest {
    @Test
    fun `User password should match correctly`(){
        val directory = Directory()
        val userTransaction: UserTransaction = directory.UserTransactionMock()
        val user = userTransaction.getUserToken(UserId(0))
    }

}