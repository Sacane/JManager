package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.infrastructure.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.api.tag.UserTagRequest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TagControllerTest (
    @LocalServerPort val port: Int,
    @Autowired val state: TagStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper
): AuthenticatedUserTest() {

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
    }
    /**
     *     @PostMapping
     *     fun addPersonalTag(
     *         @RequestBody userTagDTO: UserTagDTO,
     *         @RequestHeader("Authorization") token: String
     *     ): ResponseEntity<TagDTO>
     *     = tagFeature.addTag(userId = userTagDTO.userId.id(), token = token.asTokenUUID(), userTagDTO.tagLabel.asPersonalTag(userTagDTO.colorDTO.asAwtColor()))
     *             .map { it.toDTO() }.toHttpResponse()
     *
     *
     *     @GetMapping("/user/{userId}")
     *     fun getAllTags(
     *         @RequestHeader("Authorization") token: String,
     *         @PathVariable("userId") userId: Long
     *     ): ResponseEntity<List<TagDTO>>
     *     = tagFeature.getAllTags(userId.id(), token.asTokenUUID()).map { it.map { tag -> tag.toDTO() } }.toHttpResponse()
     *
     *
     *     @DeleteMapping("{tagId}/user/{userId}")
     *     fun deleteTag(
     *         @RequestHeader("Authorization") token: String,
     *         @PathVariable("userId") userId: Long,
     *         @PathVariable("tagId") tagId: Long
     *     ): ResponseEntity<Nothing>
     *        = tagFeature.deleteTag(userId.id(), token.asTokenUUID(), tagId)
     *            .toHttpResponse()
     *
     *     @GetMapping("/user/{userId}/default")
     *     fun defaultTag(
     *         @RequestHeader("Authorization") token: String,
     *         @PathVariable("userId") userId: Long,
     *     ): ResponseEntity<TagDTO> = tagFeature.defaultTag(userId.id(), token.asTokenUUID())
     *         .map { it.toDTO() }.toHttpResponse()
     *
     */


}