package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.TransactionFixture
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.input.tag.*
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TagFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val tagState = factory.tagTestState()
    private val addTagUseCase: AddTagUseCase = factory.addTagUseCase()
    private val deleteTagUseCase: DeleteTagUseCase = factory.deleteTagUseCase()
    private val getAllTagsUseCase: GetAllTagsUseCase = factory.getAllTagsUseCase()
    private val addDefaultTagsUseCase: AddDefaultTagsUseCase = factory.addDefaultTagsUseCase()
    private val editTagUseCase: EditTagUseCase = factory.editTagUseCase()
    private val defaultTagUseCase: DefaultTagUseCase = factory.defaultTagUseCase()

    @AfterEach
    fun clearUp() {
        factory.clearAll()
    }

    @Nested
    inner class AddTagFeatureTest {

        @Test
        fun `Add a tag must return success`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { addTagUseCase.handle(AddTagCommand(ctx.userId, Tag.Personal("test"))) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Add a tag with the same label must return failure`() {
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("test"))

            val result = act { addTagUseCase.handle(AddTagCommand(ctx.userId, Tag.Personal("test"))) }

            then(result) { assertFailure() }
        }

        @Test
        fun `Add a default tag must return failure`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { addTagUseCase.handle(AddTagCommand(ctx.userId, Tag.Default("test"))) }

            then(result) {
                assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
                assertEquals("domain.tag.add.label_already_taken", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class DeleteTagFeatureTest {
        @Test
        fun `Delete a tag must return success`() {
            val uuid = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("test", id = uuid))

            val result = act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, uuid, false)) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Delete a tag that does not exist must return failure`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, UUID.randomUUID(), false)) }

            then(result) { assertFailure() }
        }

        @Test
        fun `Delete a personal tag used in a transaction without force must return TAG_IN_USE`() {
            val tagId = UUID.randomUUID()
            val tag = Tag.Personal("usedTag", id = tagId)
            val tx = TransactionFixture.aTransaction(label = "tx1", amount = Amount(50L), isIncome = false, tag = tag)
            val ctx = scenario.withUser().withBooklet()
                .withTags(tag)
                .withTransactions(tx)

            val result = act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, tagId, false)) }

            then(result) {
                assertFailure(ResultState.TAG_IN_USE)
                assertEquals("domain.tag.delete.tag_in_use", errorInfo?.key)
            }
        }

        @Test
        fun `Delete a personal tag used in a transaction with force must succeed and reassign to default tag`() {
            val tagId = UUID.randomUUID()
            val tag = Tag.Personal("usedTag", id = tagId)
            val tx = TransactionFixture.aTransaction(label = "tx1", amount = Amount(50L), isIncome = false, tag = tag)
            val ctx = scenario.withUser().withBooklet()
                .withTags(tag)
                .withTransactions(tx)

            act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, tagId, true)).assertSuccess() }

            val updated = factory.transactionRepository().findTransactionById(tx.id!!)
            assertTrue(updated?.tag?.isDefault == true, "Transaction tag should be replaced with the default tag")
        }

        @Test
        fun `Delete a parent tag with force must also delete its sub-tags`() {
            val parentId = UUID.randomUUID()
            val subTagId1 = UUID.randomUUID()
            val subTagId2 = UUID.randomUUID()
            val parentTag = Tag.Personal("Food", id = parentId)
            val subTag1 = Tag.Personal("Restaurants", id = subTagId1, parentId = parentId)
            val subTag2 = Tag.Personal("Groceries", id = subTagId2, parentId = parentId)
            val tx = TransactionFixture.aTransaction(label = "tx1", amount = Amount(50L), isIncome = false, tag = subTag1)
            val ctx = scenario.withUser().withBooklet()
                .withTags(parentTag, subTag1, subTag2)
                .withTransactions(tx)

            act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, parentId, true)).assertSuccess() }

            val remainingTags = tagState.getStates().filter { it is Tag.Personal }
            assertTrue(remainingTags.none { it.id == parentId }, "Parent tag should be deleted")
            assertTrue(remainingTags.none { it.id == subTagId1 }, "Sub-tag 1 should be deleted")
            assertTrue(remainingTags.none { it.id == subTagId2 }, "Sub-tag 2 should be deleted")

            val updatedTx = factory.transactionRepository().findTransactionById(tx.id!!)
            assertTrue(updatedTx?.tag?.isDefault == true, "Sub-tag transaction should be reassigned to default")
        }

        @Test
        fun `Delete a parent tag without force when sub-tag is used must return TAG_IN_USE`() {
            val parentId = UUID.randomUUID()
            val subTagId = UUID.randomUUID()
            val parentTag = Tag.Personal("Food", id = parentId)
            val subTag = Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
            val tx = TransactionFixture.aTransaction(label = "tx1", amount = Amount(20L), isIncome = false, tag = subTag)
            val ctx = scenario.withUser().withBooklet()
                .withTags(parentTag, subTag)
                .withTransactions(tx)

            val result = act { deleteTagUseCase.handle(DeleteTagCommand(ctx.userId, parentId, false)) }

            then(result) { assertFailure(ResultState.TAG_IN_USE) }
        }
    }

    @Nested
    inner class GetTagsFeatureTest {
        @Test
        fun `Get all tags must return success`() {
            val uuid = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("test", id = uuid))

            val result = act { getAllTagsUseCase.handle(GetAllTagsQuery(ctx.userId)) }

            then(result) { assertSuccess() }
        }
    }

    @Nested
    inner class AddDefaultTagsFeatureTest {
        @Test
        fun `Add default tags must return success`() {
            act { addDefaultTagsUseCase.handle() }

            assertEquals(defaultTags.size, tagState.getStates().size)
        }

        @Test
        fun `Add default tags must return success and should not add duplicated tags`() {
            addDefaultTagsUseCase.handle()

            act { addDefaultTagsUseCase.handle() }

            assertEquals(defaultTags.size, tagState.getStates().size)
        }
    }

    @Nested
    inner class PatchTagFeatureTest {
        @Test
        fun `Patch a tag must return success`() {
            val uuid = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("test", id = uuid))

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("test2", id = uuid))) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Patch a tag that does not exist must return failure`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("test2", id = UUID.randomUUID()))) }

            then(result) { assertFailure() }
        }

        @Test
        fun `Patch a tag without id must return failure`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("no-id"))) }

            then(result) {
                assertFailure(ResultState.NOT_FOUND)
                assertEquals("domain.tag.edit.not_found", errorInfo?.key)
            }
        }

        @Test
        fun `Patch a tag when another tag has the same label must return failure`() {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(Tag.Personal("one", id = id1), Tag.Personal("two", id = id2))

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("two", id = id1))) }

            then(result) { assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN) }
        }

        @Test
        fun `Patch a tag that is default must return failure`() {
            val uuid = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("test", id = uuid))

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Default("test", id = uuid))) }

            then(result) {
                assertFailure(ResultState.TAG_SHOULD_NOT_BE_DEFAULT)
                assertEquals("domain.tag.edit.default_forbidden", errorInfo?.key)
            }
        }

        @Test
        fun `Patch a sub-tag with null parentId must promote it to top-level tag`() {
            val parentId = UUID.randomUUID()
            val subTagId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("Restaurants", id = subTagId, parentId = null))) }

            then(result) {
                assertSuccess()
                onSuccess { tag ->
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
            val parentId = UUID.randomUUID()
            val subTagId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Fast Food", id = subTagId, parentId = parentId)
                )

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("Dining", id = subTagId, parentId = null))) }

            then(result) {
                assertSuccess()
                val persisted = tagState.getStates().find { it.id == subTagId } as? Tag.Personal
                assertNotNull(persisted)
                assertEquals("Dining", persisted!!.label)
                assertNull(persisted.parentId)
            }
        }

        @Test
        fun `Patch a sub-tag keeping its parentId must remain a sub-tag`() {
            val parentId = UUID.randomUUID()
            val subTagId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )

            val result = act { editTagUseCase.handle(EditTagCommand(ctx.userId, Tag.Personal("Restaurants Updated", id = subTagId, parentId = parentId))) }

            then(result) {
                assertSuccess()
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
            val ctx = scenario.withUser().withBooklet()

            val result = act { defaultTagUseCase.handle(DefaultTagQuery(ctx.userId)) }

            then(result) { assertTrue { label == "Aucune" } }
        }
    }

    @Nested
    inner class CreateSubTagFeatureTest {

        private val createSubTagUseCase: CreateSubTagUseCase = factory.createSubTagUseCase()

        @Test
        fun `Create a sub-tag under an existing personal parent tag must return success`() {
            val parentId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet().withTags(Tag.Personal("Food", id = parentId))

            val result = act { createSubTagUseCase.handle(CreateSubTagCommand(ctx.userId, Tag.Personal("Restaurants"), parentId)) }

            then(result) {
                assertSuccess()
                onSuccess { tag ->
                    assertTrue(tag is Tag.Personal)
                    assertEquals(parentId, (tag as Tag.Personal).parentId)
                    assertEquals("Restaurants", tag.label)
                }
            }
        }

        @Test
        fun `Create a sub-tag under a non-existent parent must return NOT_FOUND`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { createSubTagUseCase.handle(CreateSubTagCommand(ctx.userId, Tag.Personal("Restaurants"), UUID.randomUUID())) }

            then(result) {
                assertFailure(ResultState.NOT_FOUND)
                assertEquals("domain.tag.create_sub_tag.parent_not_found", errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag under a default tag must return INVALID`() {
            val ctx = scenario.withUser().withBooklet()
            val defaultTag = factory.fakeTagRepository().defaultTag()

            val result = act { createSubTagUseCase.handle(CreateSubTagCommand(ctx.userId, Tag.Personal("SubDefault"), defaultTag.id!!)) }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.tag.create_sub_tag.parent_is_default", errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag under another sub-tag must return TAG_PARENT_IS_SUBTAG`() {
            val parentId = UUID.randomUUID()
            val subTagId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = subTagId, parentId = parentId)
                )

            val result = act { createSubTagUseCase.handle(CreateSubTagCommand(ctx.userId, Tag.Personal("FastFood"), subTagId)) }

            then(result) {
                assertFailure(ResultState.TAG_PARENT_IS_SUBTAG)
                assertEquals("domain.tag.create_sub_tag.parent_is_subtag", errorInfo?.key)
            }
        }

        @Test
        fun `Create a sub-tag with duplicate label must return TAG_LABEL_ALREADY_TAKEN`() {
            val parentId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = UUID.randomUUID())
                )

            val result = act { createSubTagUseCase.handle(CreateSubTagCommand(ctx.userId, Tag.Personal("Restaurants"), parentId)) }

            then(result) {
                assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
                assertEquals("domain.tag.create_sub_tag.label_already_taken", errorInfo?.key)
            }
        }

        @Test
        fun `GetAllTags returns sub-tags with parentId set`() {
            val parentId = UUID.randomUUID()
            val ctx = scenario.withUser().withBooklet()
                .withTags(
                    Tag.Personal("Food", id = parentId),
                    Tag.Personal("Restaurants", id = UUID.randomUUID(), parentId = parentId)
                )

            val result = act { getAllTagsUseCase.handle(GetAllTagsQuery(ctx.userId)) }

            then(result) {
                assertSuccess()
                onSuccess { tags ->
                    val restaurants = tags.filterIsInstance<Tag.Personal>().find { it.label == "Restaurants" }
                    assertEquals(parentId, restaurants?.parentId)
                    val food = tags.filterIsInstance<Tag.Personal>().find { it.label == "Food" }
                    assertEquals(null, food?.parentId)
                }
            }
        }
    }
}
