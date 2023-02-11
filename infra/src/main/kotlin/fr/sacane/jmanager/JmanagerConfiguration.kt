package fr.sacane.jmanager

import fr.sacane.jmanager.domain.port.api.BudgetResolver
import fr.sacane.jmanager.domain.port.api.LoginManager
import fr.sacane.jmanager.domain.port.api.BudgetResolverApply
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
    fun transactionReaderAdapter(serverAdapter: TransactionRegister, userTransaction: UserTransaction): BudgetResolver {
        return BudgetResolverApply(serverAdapter, userTransaction)
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