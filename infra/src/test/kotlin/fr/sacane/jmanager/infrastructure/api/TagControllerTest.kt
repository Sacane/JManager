package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.infrastructure.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.UserTagsRequest

import fr.sacane.jmanager.infrastructure.api.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.api.tag.UserTagRequest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.awt.Color


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TagControllerTest (
    @LocalServerPort val port: Int,
    @Autowired val state: TagStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper
): AuthenticatedUserTest() {

    @Autowired
    private lateinit var tagStateTestAdapter: TagStateTestAdapter

    @AfterEach
    fun clear() {
        state.clear()
    }
    @Nested
    inner class AddPersonalTagEndpointTest {
        @Test
        fun `Add personal tag endpoint successfully must return 200`() {
            val body = UserTagRequest(
                user!!.id.value!!,
                "Test",
                ColorDTO(10, 20, 30)
            )
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Test"),
                    "colorDTO.red", equalTo(10),
                    "colorDTO.green", equalTo(20),
                    "colorDTO.blue", equalTo(30),
                )
            }
            state.get().find { it.label == "Test" }.apply { assertNotNull(this) }
        }
        @Test
        fun `Add personal tag with existing label for the same profile should return 403`() {
            tagStateTestAdapter.init(
                listOf(
                    UserTagsRequest(
                        user!!.id, listOf(Tag("test", color = Color(10, 10, 10)))
                    )
                )
            )
            val body = UserTagRequest(
                user!!.id.value!!,
                "test",
                ColorDTO(10, 20, 30)
            )
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag")
            } Then {
                statusCode(403)
            }
        }
    }
    @Nested
    inner class GetAllRegisteredTags {

        @Test
        fun `Request for tags with no registered personal tags must return at least the default ones`(){
            Given {
                port(port)
                header("Authorization", token)
            } When {
                get("/api/tag/user/${user!!.id.value}")
            } Then {
                statusCode(200)
                body(
                    "size()", equalTo(defaultTags.size),
                    "any { it.label == 'Aucune' }", equalTo(true),
                    "any { it.label == 'Loisir' }", equalTo(true),
                )
            }
        }

        @Test
        fun `Get all registered tags should return 200 and body should contain default and personal tags`() {
            val initialState = listOf(
                UserTagsRequest(
                    user!!.id, listOf(Tag("test", color = Color(10, 10, 10)))
                ),
                UserTagsRequest(
                    user!!.id, listOf(Tag("test2", color = Color(20, 20, 20)))
                )
            )
            tagStateTestAdapter.init(
                initialState
            )
            Given {
                port(port)
                header("Authorization", token)
            } When {
                get("/api/tag/user/${user!!.id.value}")
            } Then {
                statusCode(200)
                body(
                    "size()", equalTo(defaultTags.size + initialState.size),
                    "any { it.label == 'test' }", equalTo(true),
                    "any { it.label == 'test2' }", equalTo(true),
                )
            }
        }
    }
    @Nested
    inner class DeleteTagEndpointTest {
        @Test
        fun `Delete tag endpoint successfully must return 200`() {
            val initialState = listOf(
                UserTagsRequest(
                    user!!.id, listOf(Tag("test", color = Color(10, 10, 10)))
                )
            )
            tagStateTestAdapter.init(
                initialState
            )
            val targetTag = state.get().find { it.label == "test" } ?: fail("Tag not found")
            Given {
                port(port)
                header("Authorization", token)
            } When {
                delete("/api/tag/${targetTag.id}/user/${user!!.id.value}")
            } Then {
                statusCode(200)
            }
            println(state.get())
            state.get().find { it.label == "test" }.apply { assertNull(this) }
        }
        @Test
        fun `Delete tag endpoint with non existing tag should return 404`() {
            Given {
                port(port)
                header("Authorization", token)
            } When {
                delete("/api/tag/1/user/${user!!.id.value}")
            } Then {
                statusCode(404)
            }
        }
    }

}