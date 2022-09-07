package fr.sacane.jmanager.infra.api.adapters.controllers

import fr.sacane.jmanager.domain.port.apiside.ApiPort
import org.springframework.beans.factory.annotation.Autowired

class JManagerController {

    @Autowired
    private lateinit var domainPort: ApiPort




}