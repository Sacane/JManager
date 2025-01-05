package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.port.api.TagFeature
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
    }
}