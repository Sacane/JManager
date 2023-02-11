package fr.sacane.jmanager

import fr.sacane.jmanager.domain.port.api.*
import fr.sacane.jmanager.domain.port.spi.LoginInventory
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import fr.sacane.jmanager.server.adapters.LoginTransactionAdapter
import fr.sacane.jmanager.server.adapters.ServerTransactionAdapter
import fr.sacane.jmanager.server.adapters.ServerUserAdapter
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
class JmanagerConfiguration {


    @Bean
    fun userTransaction(): UserTransaction{
        return ServerUserAdapter()
    }
    @Bean
    fun serverAdapter(): TransactionRegister{
        return ServerTransactionAdapter()
    }
    @Bean
    fun loginTransactionInventory(): LoginInventory{
        return LoginTransactionAdapter()
    }
    @Bean
    fun transactionReaderAdapter(serverAdapter: TransactionRegister, userTransaction: UserTransaction): BudgetResolver {
        return BudgetResolverApply(serverAdapter, userTransaction)
    }
    @Bean
    fun loginManager(loginTransaction: LoginInventory, userTransaction: UserTransaction) : Administrator {
        return LoginManager(loginTransaction, userTransaction)
    }
    @Bean
    fun userRegister(userTransaction: UserTransaction): UserRegister {
        return UserRegister(userTransaction)
    }

}