package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.input.tag.*
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TagFeatureTest: FeatureTest() {

    private val tagState = FakeFactory.tagTestState()
    private val addTagUseCase: AddTagUseCase = FakeFactory.addTagUseCase()
    private val deleteTagUseCase: DeleteTagUseCase = FakeFactory.deleteTagUseCase()
    private val getAllTagsUseCase: GetAllTagsUseCase = FakeFactory.getAllTagsUseCase()
    private val addDefaultTagsUseCase: AddDefaultTagsUseCase = FakeFactory.addDefaultTagsUseCase()
    private val editTagUseCase: EditTagUseCase = FakeFactory.editTagUseCase()
    private val defaultTagUseCase: DefaultTagUseCase = FakeFactory.defaultTagUseCase()

    @AfterEach
    fun clearUp() {
        tagState.clear()
    }

    @Nested
    inner class AddTagFeatureTest {

        @Test
        fun `Add a tag must return success`() {
            launchWithUserId {
                addTagUseCase.handle(AddTagCommand(this.userId, Tag.Personal("test")))
                    .assertSuccess()
            }
        }
        @Test
        fun `Add a tag with the same label must return failure`() {
            launchWithUserId {
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("test"))))

                addTagUseCase.handle(AddTagCommand(this.userId, Tag.Personal("test")))
                    .assertFailure()
            }
        }

        @Test
        fun `Add a default tag must return failure`() {
            launchWithUserId {
                val result = addTagUseCase.handle(AddTagCommand(this.userId, Tag.Default("test")))

                result.assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
                assertEquals("domain.tag.add.label_already_taken", result.errorInfo?.key)
            }
        }
    }
    @Nested
    inner class DeleteTagFeatureTest {
        @Test
        fun `Delete a tag must return success`() {
            launchWithUserId {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("test", id = uuid))))

                deleteTagUseCase.handle(DeleteTagCommand(this.userId, uuid, false))
                    .assertSuccess()
            }
        }
        @Test
        fun `Delete a tag that does not exist must return failure`() {
            launchWithUserId {
                deleteTagUseCase.handle(DeleteTagCommand(this.userId, UUID.randomUUID(), false))
                    .assertFailure()
            }
        }

        @Test
        fun `Delete a personal tag used in a transaction without force must return TAG_IN_USE`() {
            launchWithUserId {
                val tagId = UUID.randomUUID()
                val tag = Tag.Personal("usedTag", id = tagId)
                tagState.init(UserTag(this.userId, mutableListOf(tag)))
                initTransactions(listOf(generateTransaction("tx1", Amount(50L), false, tag = tag)))

                val result = deleteTagUseCase.handle(DeleteTagCommand(this.userId, tagId, false))

                result.assertFailure(ResultState.TAG_IN_USE)
                assertEquals("domain.tag.delete.tag_in_use", result.errorInfo?.key)
            }
        }

        @Test
        fun `Delete a personal tag used in a transaction with force must succeed and reassign to default tag`() {
            launchWithUserId {
                val tagId = UUID.randomUUID()
                val tag = Tag.Personal("usedTag", id = tagId)
                tagState.init(UserTag(this.userId, mutableListOf(tag)))
                val transaction = generateTransaction("tx1", Amount(50L), false, tag = tag)
                initTransactions(listOf(transaction))

                deleteTagUseCase.handle(DeleteTagCommand(this.userId, tagId, true))
                    .assertSuccess()

                val updated = FakeFactory.transactionRepository().findTransactionById(transaction.id!!)
                assertTrue(updated?.tag?.isDefault == true, "Transaction tag should be replaced with the default tag")
            }
        }
    }
    @Nested
    inner class GetTagsFeatureTest {
        @Test
        fun `Get all tags must return success`() {
            launchWithUserId {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("test", id = uuid))))

                getAllTagsUseCase.handle(GetAllTagsQuery(this.userId))
                    .assertSuccess()
            }
        }
    }
    @Nested
    inner class AddDefaultTagsFeatureTest {
        @Test
        fun `Add default tags must return success`() {
            addDefaultTagsUseCase.handle()

            assertEquals(defaultTags.size, tagState.getStates().size)
        }

        @Test
        fun `Add default tags must return success and should not add duplicated tags`() {
            addDefaultTagsUseCase.handle()
            addDefaultTagsUseCase.handle()

            assertEquals(defaultTags.size, tagState.getStates().size)
        }
    }
    @Nested
    inner class PatchTagFeatureTest {
        @Test
        fun `Patch a tag must return success`() {
            launchWithUserId {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("test", id = uuid))))

                editTagUseCase.handle(EditTagCommand(this.userId, Tag.Personal("test2", id = uuid)))
                    .assertSuccess()
            }
        }
        @Test
        fun `Patch a tag that does not exist must return failure`() {
            launchWithUserId {
                editTagUseCase.handle(EditTagCommand(this.userId, Tag.Personal("test2", id = UUID.randomUUID())))
                    .assertFailure()
            }
        }

        @Test
        fun `Patch a tag without id must return failure`() {
            launchWithUserId {
                val result = editTagUseCase.handle(EditTagCommand(this.userId, Tag.Personal("no-id")))

                result.assertFailure(ResultState.NOT_FOUND)
                assertEquals("domain.tag.edit.not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `Patch a tag when another tag has the same label must return failure`() {
            launchWithUserId {
                val id1 = UUID.randomUUID()
                val id2 = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("one", id = id1), Tag.Personal("two", id = id2))))

                editTagUseCase.handle(EditTagCommand(this.userId, Tag.Personal("two", id = id1)))
                    .assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
            }
        }

        @Test
        fun `Patch a tag that is default must return failure`() {
            launchWithUserId {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("test", id = uuid))))

                val result = editTagUseCase.handle(EditTagCommand(this.userId, Tag.Default("test", id = uuid)))

                result.assertFailure(ResultState.TAG_SHOULD_NOT_BE_DEFAULT)
                assertEquals("domain.tag.edit.default_forbidden", result.errorInfo?.key)
            }
        }
    }

    @Nested
    inner class DefaultTagFeatureTest {
        @Test
        fun `Default tag must return success`() {
            launchWithUserId {
                defaultTagUseCase.handle(DefaultTagQuery(this.userId))
                    .assertTrue { label == "Aucune" }
            }
        }
    }
}