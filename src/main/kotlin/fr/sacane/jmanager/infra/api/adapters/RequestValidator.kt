package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.adapter.UserRegisterAdapter
import fr.sacane.jmanager.domain.model.Response
import fr.sacane.jmanager.domain.model.TicketState
import fr.sacane.jmanager.domain.port.apiside.UserRegister
import fr.sacane.jmanager.infra.api.RegisteredUserDTO
import fr.sacane.jmanager.infra.api.UserDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class RequestValidator{
    @Autowired
    private lateinit var userFlow: UserRegisterAdapter

    fun registerUser(userDTO: RegisteredUserDTO): ResponseEntity<UserDTO>{
        val response = userFlow.signIn(userDTO.toModel())
        return when(response.status){
            TicketState.OK -> Response.ok(response.get()!!.toDTO()).toResponseEntity()
            TicketState.INVALID -> Response.invalid().toResponseEntity()
            TicketState.TIMEOUT -> Response.timeout().toResponseEntity()
        }
    }
}