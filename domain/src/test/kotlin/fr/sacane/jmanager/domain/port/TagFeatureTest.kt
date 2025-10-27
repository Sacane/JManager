package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TagFeatureTest: FeatureTest() {

    private val tagState = FakeFactory.tagTestState()
    private val tagFeature: TagFeature = FakeFactory.tagFeature()

    @AfterEach
    fun clearUp() {
        tagState.clear()
    }

    @Nested
    inner class AddTagFeatureTest {

        @Test
        fun `Add a tag must return success`() {
            launchWithConnectedUserInstance {
                tagFeature.addTag(this.tokenValue, Tag("test"))
                    .assertSuccess()
            }
        }
        @Test
        fun `Add a tag with the same label must return failure`() {
            launchWithConnectedUserInstance {
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("test"))))

                tagFeature.addTag(this.tokenValue, Tag("test"))
                    .assertFailure()
            }
        }

        @Test
        fun `Add a default tag must return failure`() {
            launchWithConnectedUserInstance {
                tagFeature.addTag(this.tokenValue, Tag("test", isDefault = true))
                    .assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
            }
        }
    }
    @Nested
    inner class DeleteTagFeatureTest {
        @Test
        fun `Delete a tag must return success`() {
            launchWithConnectedUserInstance {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("test", id = uuid))))

                tagFeature.deleteTag(this.tokenValue, uuid)
                    .assertSuccess()
            }
        }
        @Test
        fun `Delete a tag that does not exist must return failure`() {
            launchWithConnectedUserInstance {
                tagFeature.deleteTag(this.tokenValue, UUID.randomUUID())
                    .assertFailure()
            }
        }
    }
    @Nested
    inner class GetTagsFeatureTest {
        @Test
        fun `Get all tags must return success`() {
            launchWithConnectedUserInstance {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("test", id = uuid))))

                tagFeature.getAllTags(this.tokenValue)
                    .assertSuccess()
            }
        }
    }
    @Nested
    inner class AddDefaultTagsFeatureTest {
        @Test
        fun `Add default tags must return success`() {
            tagFeature.addDefaultTags()

            assertEquals(defaultTags.size, tagState.getStates().size)
        }

        @Test
        fun `Add default tags must return success and should not add duplicated tags`() {
            tagFeature.addDefaultTags()
            tagFeature.addDefaultTags()

            assertEquals(defaultTags.size, tagState.getStates().size)
        }
    }
    @Nested
    inner class PatchTagFeatureTest {
        @Test
        fun `Patch a tag must return success`() {
            launchWithConnectedUserInstance {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("test", id = uuid))))

                tagFeature.editTag(this.tokenValue, Tag("test2", id = uuid))
                    .assertSuccess()
            }
        }
        @Test
        fun `Patch a tag that does not exist must return failure`() {
            launchWithConnectedUserInstance {
                tagFeature.editTag(this.tokenValue, Tag("test2", id = UUID.randomUUID()))
                    .assertFailure()
            }
        }

        @Test
        fun `Patch a tag without id must return failure`() {
            launchWithConnectedUserInstance {
                tagFeature.editTag(this.tokenValue, Tag("no-id"))
                    .assertFailure(ResultState.NOT_FOUND)
            }
        }

        @Test
        fun `Patch a tag when another tag has the same label must return failure`() {
            launchWithConnectedUserInstance {
                val id1 = UUID.randomUUID()
                val id2 = UUID.randomUUID()
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("one", id = id1), Tag("two", id = id2))))

                tagFeature.editTag(this.tokenValue, Tag("two", id = id1))
                    .assertFailure(ResultState.TAG_LABEL_ALREADY_TAKEN)
            }
        }

        @Test
        fun `Patch a tag that is default must return failure`() {
            launchWithConnectedUserInstance {
                val uuid = UUID.randomUUID()
                tagState.init(UserTag(this.user.id, mutableListOf(Tag("test", id = uuid))))

                tagFeature.editTag(this.tokenValue, Tag("test", id = uuid, isDefault = true))
                    .assertFailure(ResultState.TAG_SHOULD_NOT_BE_DEFAULT)
            }
        }
    }

    @Nested
    inner class DefaultTagFeatureTest {
        @Test
        fun `Default tag must return success`() {
            launchWithConnectedUserInstance {
                tagFeature.defaultTag(this.tokenValue)
                    .assertTrue { label == "Aucune" }
            }
        }
    }
}