package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.application.api.setup.BookletTransaction
import fr.sacane.jmanager.application.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.application.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.application.api.setup.UserTagsRequest

import fr.sacane.jmanager.application.api.tag.ColorDTO
import fr.sacane.jmanager.application.api.tag.CreateSubTagRequest
import fr.sacane.jmanager.application.api.tag.TagDTO
import fr.sacane.jmanager.application.api.tag.UserTagRequest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.awt.Color
import java.time.LocalDate
import java.util.UUID


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TagControllerTest (
    @LocalServerPort val port: Int,
    @Autowired val state: TagStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired val transactionStateTestAdapter: TransactionStateTestAdapter
): AuthenticatedUserTest() {

    @Autowired
    private lateinit var tagStateTestAdapter: TagStateTestAdapter

    @AfterEach
    fun clear() {
        transactionStateTestAdapter.clear()
        bookletStateTestAdapter.clear()
        state.clear()
    }
    @Nested
    inner class AddPersonalTagEndpointTest {
        @Test
        fun `Add personal tag endpoint successfully must return 200`() {
            val body = UserTagRequest(
                "Test",
                ColorDTO(10, 20, 30)
            )
            Given {
                port(port)
                cookie("token", token)
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
                        user!!.id, listOf(Tag.Personal("test", color = Color(10, 10, 10)))
                    )
                )
            )
            val body = UserTagRequest(
                "test",
                ColorDTO(10, 20, 30)
            )
            Given {
                port(port)
                cookie("token", token)
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
                cookie("token", token)
            } When {
                get("/api/tag")
            } Then {
                statusCode(200)
                body(
                    "size()", equalTo(defaultTags.size),
                    "any { it.label == 'Aucune' }", equalTo(true),
                )
            }
        }

        @Test
        fun `Get all registered tags should return 200 and body should contain default and personal tags`() {
            val initialState = listOf(
                UserTagsRequest(
                    user!!.id, listOf(Tag.Personal("test", color = Color(10, 10, 10)))
                ),
                UserTagsRequest(
                    user!!.id, listOf(Tag.Personal("test2", color = Color(20, 20, 20)))
                )
            )
            tagStateTestAdapter.init(
                initialState
            )
            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/tag")
            } Then {
                statusCode(200)
                body(
                    "size()", equalTo(defaultTags.size + initialState.size),
                    "any { it.label == 'test' }", equalTo(true),
                    "any { it.label == 'test2' }", equalTo(true),
                )
            }
        }
        @Test
        fun `GET only default tags must return them with status 200`() {
            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/tag/default")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Aucune"),
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
                    user!!.id, listOf(Tag.Personal("test", color = Color(10, 10, 10)))
                )
            )
            tagStateTestAdapter.init(
                initialState
            )
            val targetTag = state.get().find { it.label == "test" } ?: fail("Tag not found")
            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/tag/${targetTag.id}")
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
                cookie("token", token)
            } When {
                delete("/api/tag/1")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Delete tag used in a transaction without force must return 409`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(Tag.Personal("reserved", color = Color(10, 10, 10))))
            ))
            val targetTag = state.get().find { it.label == "reserved" } ?: fail("Tag not found")
            bookletStateTestAdapter.init(listOf(Booklet(Amount(0L), "test-booklet", owner = user)))
            transactionStateTestAdapter.init(listOf(
                BookletTransaction(user!!.id, "test-booklet",
                    listOf(Transaction(null, "tx1", LocalDate.now(), Amount(10L), false, tag = targetTag)),
                    token)
            ))

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/tag/${targetTag.id}")
            } Then {
                statusCode(409)
            }
        }

        @Test
        fun `Delete tag used in a transaction with force must return 200 and reassign to default tag`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(Tag.Personal("reserved", color = Color(10, 10, 10))))
            ))
            val targetTag = state.get().find { it.label == "reserved" } ?: fail("Tag not found")
            bookletStateTestAdapter.init(listOf(Booklet(Amount(0L), "test-booklet", owner = user)))
            transactionStateTestAdapter.init(listOf(
                BookletTransaction(user!!.id, "test-booklet",
                    listOf(Transaction(null, "tx1", LocalDate.now(), Amount(10L), false, tag = targetTag)),
                    token)
            ))

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/tag/${targetTag.id}?force=true")
            } Then {
                statusCode(200)
            }

            state.get().find { it.label == "reserved" }.apply { assertNull(this) }
            val transactions = transactionStateTestAdapter.get()
            assertTrue(transactions.all { it.tag?.isDefault == true }, "All transactions should now use the default tag")
        }
    }

    @Nested
    inner class PatchTagEndpointTest {
        @Test
        fun `Patch tag endpoint successfully must return 200`() {
            val element = Tag.Personal(label = "test", color = Color(10, 10, 10))
            val initialState = listOf(
                UserTagsRequest(
                    user!!.id, listOf(element)
                )
            )
            tagStateTestAdapter.init(
                initialState
            )
            val targetTag = state.get().find { it.label == "test" } ?: fail("Tag not found")
            val body = targetTag.toDTO().copy(
                label = "test2",
                colorDTO = ColorDTO(20, 20, 20)
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("test2"),
                    "colorDTO.red", equalTo(20),
                    "colorDTO.green", equalTo(20),
                    "colorDTO.blue", equalTo(20),
                )
            }
        }

        @Test
        fun `Patch tag endpoint with non existing tag should return 404`() {
            val body = TagDTO(
                tagId = UUID.randomUUID().toString(),
                label = "test",
                colorDTO = ColorDTO(10, 10, 10)
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `Patch tag endpoint with non existing tag should return 404 with empty body`() {
            val body = TagDTO(
                tagId = UUID.randomUUID().toString(),
                label = "test",
                colorDTO = ColorDTO(10, 10, 10)
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Patch sub-tag with null parentId must promote it to top-level tag`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(Tag.Personal("Food", color = Color(10, 10, 10))))
            ))
            val parentTag = state.get().find { it.label == "Food" } ?: fail("Parent tag not found")

            val subTagId = Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(
                    CreateSubTagRequest("Restaurants", ColorDTO(20, 20, 20), parentTag.id.toString())
                ))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(200)
            } Extract {
                jsonPath().getString("tagId")
            }

            val patchBody = TagDTO(
                tagId = subTagId,
                label = "Restaurants",
                colorDTO = ColorDTO(20, 20, 20),
                isDefault = false,
                parentId = null
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(patchBody))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(200)
                body("label", equalTo("Restaurants"))
            }

            val promoted = state.get().find { it.label == "Restaurants" } ?: fail("Promoted tag not found")
            assertNull((promoted as? Tag.Personal)?.parentId)
        }

        @Test
        fun `Patch sub-tag with null parentId and new label must promote it with updated label`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(Tag.Personal("Food", color = Color(10, 10, 10))))
            ))
            val parentTag = state.get().find { it.label == "Food" } ?: fail("Parent tag not found")

            val subTagId = Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(
                    CreateSubTagRequest("Fast Food", ColorDTO(20, 20, 20), parentTag.id.toString())
                ))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(200)
            } Extract {
                jsonPath().getString("tagId")
            }

            val patchBody = TagDTO(
                tagId = subTagId,
                label = "Dining",
                colorDTO = ColorDTO(20, 20, 20),
                isDefault = false,
                parentId = null
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(patchBody))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(200)
                body("label", equalTo("Dining"))
            }

            val promoted = state.get().find { it.label == "Dining" } ?: fail("Promoted tag not found")
            assertNull((promoted as? Tag.Personal)?.parentId)
        }

        @Test
        fun `Patch tag with unknown tagId and null parentId must return 404`() {
            val body = TagDTO(
                tagId = UUID.randomUUID().toString(),
                label = "ghost",
                colorDTO = ColorDTO(10, 10, 10),
                parentId = null
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/tag")
            } Then {
                statusCode(404)
            }
        }
    }

    @Nested
    inner class CreateSubTagEndpointTest {
        @Test
        fun `Create sub-tag under an existing personal tag must return 200`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(Tag.Personal("parent", color = Color(10, 10, 10))))
            ))
            val parentTag = state.get().find { it.label == "parent" } ?: fail("Parent tag not found")

            val body = CreateSubTagRequest(
                tagLabel = "child",
                colorDTO = ColorDTO(20, 20, 20),
                parentId = parentTag.id.toString()
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("child"),
                    "colorDTO.red", equalTo(20),
                    "colorDTO.green", equalTo(20),
                    "colorDTO.blue", equalTo(20),
                    "parentId", equalTo(parentTag.id.toString()),
                )
            }
        }

        @Test
        fun `Create sub-tag with non-existing parent must return 404`() {
            val body = CreateSubTagRequest(
                tagLabel = "orphan",
                colorDTO = ColorDTO(10, 10, 10),
                parentId = UUID.randomUUID().toString()
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Create sub-tag under a default tag must return 400`() {
            val defaultTag = state.get().find { it.isDefault } ?: fail("Default tag not found")

            val body = CreateSubTagRequest(
                tagLabel = "child",
                colorDTO = ColorDTO(10, 10, 10),
                parentId = defaultTag.id.toString()
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Create sub-tag with duplicate label must return 403`() {
            tagStateTestAdapter.init(listOf(
                UserTagsRequest(user!!.id, listOf(
                    Tag.Personal("parent", color = Color(10, 10, 10)),
                    Tag.Personal("duplicate", color = Color(20, 20, 20))
                ))
            ))
            val parentTag = state.get().find { it.label == "parent" } ?: fail("Parent tag not found")

            val body = CreateSubTagRequest(
                tagLabel = "duplicate",
                colorDTO = ColorDTO(30, 30, 30),
                parentId = parentTag.id.toString()
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/tag/sub-tag")
            } Then {
                statusCode(403)
            }
        }
    }

}