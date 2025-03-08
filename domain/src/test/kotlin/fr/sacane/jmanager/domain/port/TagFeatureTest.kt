package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.api.TagFeature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TagFeatureTest: FeatureTest() {

    private val tagState = FakeFactory.fakeTagRepository()
    private val tagFeature: TagFeature = FakeFactory.tagFeature()

    @Nested
    inner class AddTagFeatureTest {

        @Test
        fun `Add a tag must return success`() {
            launchWithConnectedUserInstance {
                tagFeature.addTag(userId = this.userId, this.tokenValue, Tag("test"))
                    .assertSuccess()
            }
        }
        @Test
        fun `Add a tag with the same label must return failure`() {
            launchWithConnectedUserInstance {
                tagState.init(UserTag(this.userId, mutableListOf(Tag("test"))))

                tagFeature.addTag(userId = this.userId, this.tokenValue, Tag("test"))
                    .assertFailure()
            }
        }
    }
    @Nested
    inner class DeleteTagFeatureTest {
        @Test
        fun `Delete a tag must return success`() {
            launchWithConnectedUserInstance {
                tagState.init(UserTag(this.userId, mutableListOf(Tag("test", id = 90))))

                tagFeature.deleteTag(userId = this.userId, this.tokenValue, 90)
                    .assertSuccess()
            }
        }
        @Test
        fun `Delete a tag that does not exist must return failure`() {
            launchWithConnectedUserInstance {
                tagFeature.deleteTag(userId = this.userId, this.tokenValue, 10)
                    .assertFailure()
            }
        }
    }
    @Nested
    inner class GetTagsFeatureTest {
        @Test
        fun `Get all tags must return success`() {
            launchWithConnectedUserInstance {
                tagState.init(UserTag(this.userId, mutableListOf(Tag("test", id = 90))))

                tagFeature.getAllTags(userId = this.userId, this.tokenValue)
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
}