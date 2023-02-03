package fr.sacane.jmanager.infra

import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.apiside.UserRegister
import fr.sacane.jmanager.domain.adapter.TransactionReaderAdapter
import fr.sacane.jmanager.domain.adapter.UserRegisterAdapter
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.infra.api.TransactionValidator
import fr.sacane.jmanager.infra.api.UserControlAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class JmanagerConfiguration {

    @Bean
    fun port(serverAdapter: TransactionRegister, userAdapter: UserTransaction): TransactionReader{
        return TransactionReaderAdapter(serverAdapter, userAdapter)
    }

    @Bean
    fun portUser(userTransaction: UserTransaction, loginTransaction: LoginTransactor) : UserRegisterAdapter{
        return UserRegisterAdapter(userTransaction, loginTransaction)
    }

    @Bean
    fun apiAdapter(apiPort: TransactionReader, userPort: UserTransaction): TransactionValidator {
        return TransactionValidator(apiPort, userPort)
    }

}