package fr.sacane.jmanager.infra

import fr.sacane.jmanager.domain.port.apiside.ApiPort
import fr.sacane.jmanager.domain.port.apiside.impl.ApiPortImpl
import fr.sacane.jmanager.domain.port.serverside.ServerPort
import fr.sacane.jmanager.infra.server.adapters.ServerAdapter
import org.springframework.context.annotation.Bean

class JmanagerConfiguration {

    @Bean
    fun port(): ApiPort{
        return ApiPortImpl(ServerAdapter())
    }

    @Bean
    fun serverPort(): ServerPort{
        return ServerAdapter()
    }

}