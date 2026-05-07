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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

        @Test
        fun `Delete a parent tag with force must also delete its sub-tags`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId1 = UUID.randomUUID()
                val subTagId2 = UUID.randomUUID()
                val parentTag = Tag.Personal("Food", id = parentId)
                val subTag1 = Tag.Personal("Restaurants", id = subTagId1, parentId = parentId)
                val subTag2 = Tag.Personal("Groceries", id = subTagId2, parentId = parentId)
                tagState.init(UserTag(this.userId, mutableListOf(parentTag, subTag1, subTag2)))
                val tx = generateTransaction("tx1", Amount(50L), false, tag = subTag1)
                initTransactions(listOf(tx))

                deleteTagUseCase.handle(DeleteTagCommand(this.userId, parentId, true))
                    .assertSuccess()

                val remainingTags = tagState.getStates().filter { it is Tag.Personal }
                assertTrue(remainingTags.none { it.id == parentId }, "Parent tag should be deleted")
                assertTrue(remainingTags.none { it.id == subTagId1 }, "Sub-tag 1 should be deleted")
                assertTrue(remainingTags.none { it.id == subTagId2 }, "Sub-tag 2 should be deleted")

                val updatedTx = FakeFactory.transactionRepository().findTransactionById(tx.id!!)
                assertTrue(updatedTx?.tag?.isDefault == true, "Sub-tag transaction should be reassigned to default")
            }
        }

        @Test
        fun `Delete a parent tag without force when sub-tag is used must return TAG_IN_USE`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId = UUID.randomUUID()
                val parentTag = Tag.Personal("Food", id = parentId)
                val subTag = Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                tagState.init(UserTag(this.userId, mutableListOf(parentTag, subTag)))
                initTransactions(listOf(generateTransaction("tx1", Amount(20L), false, tag = subTag)))

                val result = deleteTagUseCase.handle(DeleteTagCommand(this.userId, parentId, false))

                result.assertFailure(ResultState.TAG_IN_USE)
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

        @Test
        fun `Patch a sub-tag with null parentId must promote it to top-level tag`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )))

                val result = editTagUseCase.handle(
                    EditTagCommand(this.userId, Tag.Personal("Restaurants", id = subTagId, parentId = null))
                )

                result.assertSuccess()
                result.onSuccess { tag ->
                    assertTrue(tag is Tag.Personal)
                    assertNull((tag as Tag.Personal).parentId)
                }
                val persisted = tagState.getStates().find { it.id == subTagId } as? Tag.Personal
                assertNotNull(persisted)
                assertNull(persisted!!.parentId)
            }
        }

        @Test
        fun `Patch a sub-tag with null parentId and new label must promote it with updated label`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Fast Food", id = subTagId, parentId = parentId)
                )))

                val result = editTagUseCase.handle(
                    EditTagCommand(this.userId, Tag.Personal("Dining", id = subTagId, parentId = null))
                )

                result.assertSuccess()
                val persisted = tagState.getStates().find { it.id == subTagId } as? Tag.Personal
                assertNotNull(persisted)
                assertEquals("Dining", persisted!!.label)
                assertNull(persisted.parentId)
            }
        }

        @Test
        fun `Patch a sub-tag keeping its parentId must remain a sub-tag`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )))

                val result = editTagUseCase.handle(
                    EditTagCommand(this.userId, Tag.Personal("Restaurants Updated", id = subTagId, parentId = parentId))
                )

                result.assertSuccess()
                val persisted = tagState.getStates().find { it.id == subTagId } as? Tag.Personal
                assertNotNull(persisted)
                assertEquals(parentId, persisted!!.parentId)
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

    @Nested
    inner class CreateSubTagFeatureTest {

        private val createSubTagUseCase: CreateSubTagUseCase = FakeFactory.createSubTagUseCase()

        @Test
        fun `Create a sub-tag under an existing personal parent tag must return success`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(Tag.Personal("Food", id = parentId))))

                val result = createSubTagUseCase.handle(
                    CreateSubTagCommand(this.userId, Tag.Personal("Restaurants"), parentId)
                )

                result.assertSuccess()
                result.onSuccess { tag ->
                    assertTrue(tag is Tag.Personal)
                    assertEquals(parentId, (tag as Tag.Personal).parentId)
                    assertEquals("Restaurants", tag.label)
                }
            }
        }

        @Test
        fun `Create a sub-tag under a non-existent parent must return NOT_FOUND`() {
            launchWithUserId {
                val result = createSubTagUseCase.handle(
                    CreateSubTagCommand(this.userId, Tag.Personal("Restaurants"), UUID.randomUUID())
                )

                result.assertFailure(ResultState.NOT_FOUND)
                assertEquals("domain.tag.create_sub_tag.parent_not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag under a default tag must return INVALID`() {
            launchWithUserId {
                val defaultTag = FakeFactory.fakeTagRepository().defaultTag()

                val result = createSubTagUseCase.handle(
                    CreateSubTagCommand(this.userId, Tag.Personal("SubDefault"), defaultTag.id!!)
                )

                result.assertFailure(ResultState.INVALID)
                assertEquals("domain.tag.create_sub_tag.parent_is_default", result.errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag under another sub-tag must return TAG_PARENT_IS_SUBTAG`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                val subTagId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )))

                val result = createSubTagUseCase.handle(
                    CreateSubTagCommand(this.userId, Tag.Personal("FastFood"), subTagId)
                )

                result.assertFailure(ResultState.TAG_PARENT_IS_SUBTAG)
                assertEquals("domain.tag.create_sub_tag.parent_is_subtag", result.errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag with duplicate label must return TAG_LABEL_ALREADY_TAKEN`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = UUID.randomUUID())
                )))

                val result = createSubTagUseCase.handle(
                    CreateSubTagCommand(this.userId, Tag.Personal("Restaurants"), parentId)
                )

                result.assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
                assertEquals("domain.tag.create_sub_tag.label_already_taken", result.errorInfo?.key)
            }
        }

        @Test
        fun `GetAllTags returns sub-tags with parentId set`() {
            launchWithUserId {
                val parentId = UUID.randomUUID()
                tagState.init(UserTag(this.userId, mutableListOf(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = UUID.randomUUID(), parentId = parentId)
                )))

                val result = getAllTagsUseCase.handle(GetAllTagsQuery(this.userId))

                result.assertSuccess()
                result.onSuccess { tags ->
                    val restaurants = tags.filterIsInstance<Tag.Personal>().find { it.label == "Restaurants" }
                    assertEquals(parentId, restaurants?.parentId)
                    val food = tags.filterIsInstance<Tag.Personal>().find { it.label == "Food" }
                    assertEquals(null, food?.parentId)
                }
            }
        }
    }
}