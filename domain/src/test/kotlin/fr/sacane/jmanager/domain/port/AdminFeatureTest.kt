package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.PaginatorImpl
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.input.admin.GetUsersQuery
import fr.sacane.jmanager.domain.port.input.admin.GetUsersService
import fr.sacane.jmanager.domain.port.input.admin.GetUsersUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class AdminFeatureTest : FeatureTest() {

    companion object {
        private val userRepository = FakeFactory.fakeUserRepository()
        private val paginator = PaginatorImpl()
        private val adminFeature: GetUsersUseCase = GetUsersService(userRepository, paginator)
    }

    @AfterEach
    fun clear() {
        FakeFactory.clearAll()
    }

    private fun createAdmin(username: String): User {
        val userId = UserId(UUID.randomUUID())
        val adminUser = User(
            id = userId,
            username = username,
            email = "$username@test.fr",
            roles = setOf(Role.USER, Role.ADMIN)
        )
        userRepository.init(listOf(UserWithPassword(adminUser, "test", roles = setOf(Role.USER, Role.ADMIN))))
        return adminUser
    }

    @Nested
    inner class GetUsersTest {

        @Test
        fun `Get users as admin should return success with paginated users`() {
            val admin = createAdmin("admin")

            val users = (1..5).map { i ->
                val userId = UserId(UUID.randomUUID())
                User(
                    id = userId,
                    username = "user$i",
                    email = "user$i@test.fr",
                    creationDate = LocalDateTime.now().minusDays(i.toLong())
                )
            }

            users.forEach { user ->
                userRepository.init(listOf(UserWithPassword(user, "test")))
            }

            adminFeature.handle(GetUsersQuery(admin.id!!, 0, 10))
                .assertTrue {
                    this.content.size == 5 && this.totalElements == 5L
                }
        }

        @Test
        fun `Get users should exclude the calling admin from results`() {
            val admin = createAdmin("admin")

            val user1 = User(UserId(UUID.randomUUID()), "user1", "user1@test.fr")
            val user2 = User(UserId(UUID.randomUUID()), "user2", "user2@test.fr")

            userRepository.init(listOf(
                UserWithPassword(user1, "test"),
                UserWithPassword(user2, "test")
            ))

            adminFeature.handle(GetUsersQuery(admin.id!!, 0, 10))
                .assertTrue {
                    this.content.size == 2 &&
                    this.content.none { it.username == "admin" }
                }
        }

        @Test
        fun `Get users with pagination should return correct page`() {
            val admin = createAdmin("admin")

            val users = (1..15).map { i ->
                User(
                    UserId(UUID.randomUUID()),
                    "user$i",
                    "user$i@test.fr",
                    creationDate = LocalDateTime.now().minusDays(i.toLong())
                )
            }

            users.forEach { user ->
                userRepository.init(listOf(UserWithPassword(user, "test")))
            }

            adminFeature.handle(GetUsersQuery(admin.id!!, 0, 5))
                .assertTrue {
                    this.content.size == 5 &&
                    this.pageNumber == 0 &&
                    this.pageSize == 5 &&
                    this.totalElements == 15L &&
                    this.totalPages == 3
                }

            adminFeature.handle(GetUsersQuery(admin.id!!, 1, 5))
                .assertTrue {
                    this.content.size == 5 &&
                    this.pageNumber == 1 &&
                    this.totalElements == 15L
                }

            adminFeature.handle(GetUsersQuery(admin.id!!, 2, 5))
                .assertTrue {
                    this.content.size == 5 &&
                    this.pageNumber == 2 &&
                    this.totalElements == 15L
                }
        }

        @Test
        fun `Get users should sort by creation date descending`() {
            val admin = createAdmin("admin")

            val user1 = User(
                UserId(UUID.randomUUID()),
                "user1",
                "user1@test.fr",
                creationDate = LocalDateTime.now().minusDays(3)
            )
            val user2 = User(
                UserId(UUID.randomUUID()),
                "user2",
                "user2@test.fr",
                creationDate = LocalDateTime.now().minusDays(1)
            )
            val user3 = User(
                UserId(UUID.randomUUID()),
                "user3",
                "user3@test.fr",
                creationDate = LocalDateTime.now().minusDays(2)
            )

            userRepository.init(listOf(
                UserWithPassword(user1, "test"),
                UserWithPassword(user2, "test"),
                UserWithPassword(user3, "test")
            ))

            adminFeature.handle(GetUsersQuery(admin.id!!, 0, 10))
                .assertTrue {
                    this.content.size == 3 &&
                    this.content[0].username == "user2" &&
                    this.content[1].username == "user3" &&
                    this.content[2].username == "user1"
                }
        }

        @Test
        fun `Get users with empty database should return empty page`() {
            val admin = createAdmin("admin")

            adminFeature.handle(GetUsersQuery(admin.id!!, 0, 10))
                .assertTrue {
                    this.content.isEmpty() &&
                    this.totalElements == 0L &&
                    this.totalPages == 0
                }
        }

        @Test
        fun `Get users with default pagination parameters`() {
            val admin = createAdmin("admin")

            val users = (1..25).map { i ->
                User(UserId(UUID.randomUUID()), "user$i", "user$i@test.fr")
            }

            users.forEach { user ->
                userRepository.init(listOf(UserWithPassword(user, "test")))
            }

            adminFeature.handle(GetUsersQuery(admin.id!!))
                .assertTrue {
                    this.pageNumber == 0 &&
                    this.pageSize == 20 &&
                    this.content.size == 20 &&
                    this.totalElements == 25L &&
                    this.totalPages == 2
                }
        }

        @Test
        fun `Get users with page beyond available pages should return empty content`() {
            val admin = createAdmin("admin")

            val user = User(UserId(UUID.randomUUID()), "user1", "user1@test.fr")
            userRepository.init(listOf(UserWithPassword(user, "test")))

            adminFeature.handle(GetUsersQuery(admin.id!!, 10, 10))
                .assertTrue {
                    this.content.isEmpty() &&
                    this.pageNumber == 10 &&
                    this.totalElements == 1L
                }
        }
    }
}

