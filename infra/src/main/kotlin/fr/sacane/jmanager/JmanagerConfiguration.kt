package fr.sacane.jmanager

import fr.sacane.jmanager.domain.port.api.LoginManager
import fr.sacane.jmanager.domain.port.api.TransactionReaderAdapter
import fr.sacane.jmanager.domain.port.api.UserRegister
import fr.sacane.jmanager.domain.port.spi.LoginInventory
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
class JmanagerConfiguration {


    @Bean
    fun transactionReaderAdapter(serverAdapter: TransactionRegister, userTransaction: UserTransaction): TransactionReaderAdapter {
        return TransactionReaderAdapter(serverAdapter, userTransaction)
    }
    @Bean
    fun loginManager(loginTransaction: LoginInventory) : LoginManager {
        return LoginManager(loginTransaction)
    }
    @Bean
    fun userRegister(userTransaction: UserTransaction): UserRegister {
        return UserRegister(userTransaction)
    }


}