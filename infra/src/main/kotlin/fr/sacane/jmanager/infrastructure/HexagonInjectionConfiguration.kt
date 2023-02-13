package fr.sacane.jmanager.infrastructure

import fr.sacane.jmanager.domain.port.api.*
import fr.sacane.jmanager.domain.port.spi.LoginManager
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableAutoConfiguration
class HexagonInjectionConfiguration {


//    @Bean
//    fun userTransaction(): UserTransaction{
//        return ServerUserAdapter()
//    }
//    @Bean
//    fun serverAdapter(): TransactionRegister{
//        return ServerTransactionAdapter()
//    }
//    @Bean
//    fun loginTransactionInventory(): LoginInventory{
//        return LoginTransactionAdapter()
//    }
    @Bean
    fun transactionReaderAdapter(serverAdapter: TransactionRegister, userTransaction: UserTransaction): BudgetResolver {
        return BudgetResolverApply(serverAdapter, userTransaction)
    }
    @Bean
    fun loginManager(loginTransaction: LoginManager, userTransaction: UserTransaction) : Administrator {
        return LoginManager(loginTransaction, userTransaction)
    }

}