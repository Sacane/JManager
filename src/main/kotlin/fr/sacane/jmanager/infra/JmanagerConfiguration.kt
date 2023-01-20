package fr.sacane.jmanager.infra

import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.apiside.impl.TransactionReaderImpl
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.infra.api.TransactionReaderAdapter
import fr.sacane.jmanager.infra.api.UserControlAdapter
import fr.sacane.jmanager.infra.server.adapters.ServerTransactionAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JmanagerConfiguration {

    @Bean
    fun port(serverAdapter: TransactionRegister, userAdapter: UserTransaction): TransactionReader{
        return TransactionReaderImpl(serverAdapter, userAdapter)
    }

    @Bean
    fun apiAdapter(apiPort: TransactionReader, userPort: UserTransaction): TransactionReaderAdapter {
        return TransactionReaderAdapter(apiPort, userPort)
    }

    @Bean
    fun userApiAdapter(adapter: UserTransaction): UserControlAdapter{
        return UserControlAdapter(adapter)
    }

}