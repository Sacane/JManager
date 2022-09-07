package fr.sacane.jmanager.infra

import fr.sacane.jmanager.domain.port.apiside.ApiPort
import fr.sacane.jmanager.domain.port.apiside.impl.ApiPortImpl
import fr.sacane.jmanager.domain.port.serverside.ServerPort
import fr.sacane.jmanager.infra.api.adapters.ApiAdapter
import fr.sacane.jmanager.infra.server.adapters.ServerAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JmanagerConfiguration {

    @Bean
    fun port(serverAdapter: ServerPort): ApiPort{
        return ApiPortImpl(serverAdapter)
    }

    @Bean
    fun apiAdapter(apiPort: ApiPort): ApiAdapter{
        return ApiAdapter(apiPort)
    }

    @Bean
    fun serverPort(): ServerPort{
        return ServerAdapter()
    }

}