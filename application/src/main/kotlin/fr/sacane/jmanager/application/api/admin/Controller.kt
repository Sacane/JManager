package fr.sacane.jmanager.application.api.admin

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.input.admin.GetUsersQuery
import fr.sacane.jmanager.domain.port.input.admin.GetUsersUseCase
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.session.UserDTO
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/admin")
@Adapter
@Validated
class AdminController(
    private val adminFeature: GetUsersUseCase
) {

    companion object {
        val LOGGER: Logger = Logger.getLogger(AdminController::class.java.name)
    }


    @GetMapping("/users")
    fun getCreatedUsers(
        @RequestParam @Min(0) page: Int,
        @RequestParam @Min(1) @Max(100) size: Int,
    ): ResponseEntity<Page<UserDTO>> {
        LOGGER.info("Fetching recently created users")
        val token = currentUser.token
        val users = adminFeature.handle(GetUsersQuery(SessionToken(token), page, size))

        return users.map {
            val content = it.content.map { user -> user.toDTO() }
            Page(content, it.pageNumber, it.pageSize, it.totalElements, it.totalPages)
        }.toHttpResponse()
    }
}