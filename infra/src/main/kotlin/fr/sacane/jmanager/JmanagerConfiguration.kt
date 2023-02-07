package fr.sacane.jmanager


import fr.sacane.jmanager.domain.port.apiside.LoginManager
import fr.sacane.jmanager.domain.port.apiside.TransactionReaderAdapter
import fr.sacane.jmanager.domain.port.apiside.UserRegister
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class JmanagerConfiguration {
    @Bean
    fun transactionReaderAdapter(serverAdapter: TransactionRegister, userTransaction: UserTransaction): TransactionReaderAdapter {
        return TransactionReaderAdapter(serverAdapter, userTransaction)
    }
    @Bean
    fun loginManager(loginTransaction: LoginTransactor) : LoginManager {
        return LoginManager(loginTransaction)
    }
    @Bean
    fun userRegister(userTransaction: UserTransaction): UserRegister {
        return UserRegister(userTransaction)
    }
}