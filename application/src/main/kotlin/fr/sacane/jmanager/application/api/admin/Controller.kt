package fr.sacane.jmanager.application.api.admin

import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.admin.AdminCreateUserCommand
import fr.sacane.jmanager.domain.port.input.admin.GetUsersQuery
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.session.UserDTO
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.bus.CommandBus
import fr.sacane.jmanager.application.bus.QueryBus
import fr.sacane.jmanager.domain.utils.ResultState
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/admin")
@Adapter
@Validated
class AdminController(
    private val queryBus: QueryBus,
    private val commandBus: CommandBus,
) {

    companion object {
        val LOGGER: Logger = Logger.getLogger(AdminController::class.java.name)
    }

    @PostMapping("/users", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createUser(@Valid @RequestBody request: AdminCreateUserRequest): ResponseEntity<UserDTO> {
        if (request.password != request.confirmPassword) {
            throw InvalidRequestException(
                ResultState.INVALID.code,
                "Les mots de passe ne correspondent pas",
                "admin.user.create.password_mismatch",
            )
        }
        return commandBus.dispatch(
            AdminCreateUserCommand(
                username = request.username,
                password = request.password,
                email = request.email,
            )
        ).map { it.toDTO() }.toHttpResponse()
    }

    @GetMapping("/users")
    fun getCreatedUsers(
        @RequestParam @Min(0) page: Int,
        @RequestParam @Min(1) @Max(100) size: Int,
    ): ResponseEntity<Page<UserDTO>> {
        LOGGER.info("Fetching recently created users")
        val users = queryBus.dispatch(GetUsersQuery(UserId(currentUser.id), page, size))

        return users.map {
            val content = it.content.map { user -> user.toDTO() }
            Page(content, it.pageNumber, it.pageSize, it.totalElements, it.totalPages)
        }.toHttpResponse()
    }
}