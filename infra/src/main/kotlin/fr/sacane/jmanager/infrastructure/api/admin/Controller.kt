package fr.sacane.jmanager.infrastructure.api.admin

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.port.api.AdminFeature
import fr.sacane.jmanager.infrastructure.api.session.UserDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/admin")
@Adapter
class AdminController(
    private val adminFeature: AdminFeature
) {

    companion object {
        val LOGGER: Logger = Logger.getLogger(AdminController::class.java.name)
    }


    @GetMapping("/users")
    fun getCreatedUsers(): ResponseEntity<List<UserDTO>> {
        LOGGER.info("Fetching recently created users")


        return ResponseEntity.ok(listOf())
    }
}