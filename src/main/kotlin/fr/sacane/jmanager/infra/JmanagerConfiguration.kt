package fr.sacane.jmanager.infra

import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.apiside.impl.TransactionReaderImpl
import fr.sacane.jmanager.domain.port.serverside.TransactionRegistrer
import fr.sacane.jmanager.infra.api.TransactionReaderAdapter
import fr.sacane.jmanager.infra.server.adapters.ServerAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JmanagerConfiguration {

    @Bean
    fun port(serverAdapter: TransactionRegistrer): TransactionReader{
        return TransactionReaderImpl(serverAdapter)
    }

    @Bean
    fun apiAdapter(apiPort: TransactionReader): TransactionReaderAdapter {
        return TransactionReaderAdapter(apiPort)
    }

    @Bean
    fun serverPort(): TransactionRegistrer{
        return ServerAdapter()
    }

}