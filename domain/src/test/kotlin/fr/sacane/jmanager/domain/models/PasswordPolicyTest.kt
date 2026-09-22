package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PasswordPolicyTest {

    @Test
    fun `shouldAcceptPassword_whenItHasTwelveCharacters`() {
        assertEquals(emptyList<PasswordRule>(), PasswordPolicy.unmetRules("correcthorse", email = "johan@example.com"))
    }

    @Test
    fun `shouldReportTooShort_whenPasswordHasElevenCharacters`() {
        assertEquals(listOf(PasswordRule.TOO_SHORT), PasswordPolicy.unmetRules("correcthors", email = "johan@example.com"))
    }

    @Test
    fun `shouldAcceptPassword_whenItHasOneHundredCharacters`() {
        assertEquals(emptyList<PasswordRule>(), PasswordPolicy.unmetRules("a".repeat(100), email = null))
    }

    @Test
    fun `shouldReportTooLong_whenPasswordHasOneHundredAndOneCharacters`() {
        assertEquals(listOf(PasswordRule.TOO_LONG), PasswordPolicy.unmetRules("a".repeat(101), email = null))
    }

    // An emoji is two UTF-16 units: counting `length` would accept an 11-character password.
    @Test
    fun `shouldCountCharactersNotCodeUnits_whenPasswordContainsAnEmoji`() {
        val elevenCharacters = "élève-été🔒x"
        assertEquals(11, elevenCharacters.codePointCount(0, elevenCharacters.length))

        assertEquals(listOf(PasswordRule.TOO_SHORT), PasswordPolicy.unmetRules(elevenCharacters, email = null))
        assertEquals(emptyList<PasswordRule>(), PasswordPolicy.unmetRules(elevenCharacters + "y", email = null))
    }

    @Test
    fun `shouldReportEqualsEmail_whenPasswordIsTheAddressInAnotherCase`() {
        assertEquals(
            listOf(PasswordRule.EQUALS_EMAIL),
            PasswordPolicy.unmetRules("johan@example.com", email = "Johan@Example.com"),
        )
    }

    @Test
    fun `shouldReportEveryUnmetRule_whenSeveralAreBroken`() {
        assertEquals(
            listOf(PasswordRule.TOO_SHORT, PasswordRule.EQUALS_EMAIL),
            PasswordPolicy.unmetRules("a@b.fr", email = "a@b.fr"),
        )
    }

    @Test
    fun `shouldIgnoreTheEmailRule_whenAccountHasNoAddress`() {
        assertEquals(emptyList<PasswordRule>(), PasswordPolicy.unmetRules("correcthorse", email = null))
    }
}
