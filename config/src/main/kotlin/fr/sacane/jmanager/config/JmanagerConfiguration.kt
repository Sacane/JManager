package fr.sacane.jmanager.config


import fr.sacane.jmanager.InfraConfig
import fr.sacane.jmanager.domain.port.apiside.LoginManager
import fr.sacane.jmanager.domain.port.apiside.TransactionReaderAdapter
import fr.sacane.jmanager.domain.port.apiside.UserRegister
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.server.adapters.LoginTransactionAdapter
import fr.sacane.jmanager.server.adapters.ServerTransactionAdapter
import fr.sacane.jmanager.server.adapters.ServerUserAdapter
import fr.sacane.jmanager.server.repositories.UserRepository
import fr.sacane.jmanager.server.spring.SpringLayerService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ComponentScan(basePackages = ["fr.sacane.jmanager.server.repositories", "fr.sacane.jmanager.server.adapters", "fr.sacane.jmanager.server.spring"])
@Import(InfraConfig::class)
class JmanagerConfiguration {

    @Bean
    fun loginTransaction(): LoginTransactor{
        return LoginTransactionAdapter()
    }
    @Bean
    fun userTransaction(): UserTransaction{
        return ServerUserAdapter()
    }
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