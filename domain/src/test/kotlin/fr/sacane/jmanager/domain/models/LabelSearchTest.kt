package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LabelSearchTest {

    @Test
    fun `shouldMatch_whenTheLabelContainsTheFragment`() {
        assertTrue(LabelSearch.of("carrefour").matches("Courses Carrefour Market"))
        assertFalse(LabelSearch.of("carrefour").matches("Essence"))
    }

    @Test
    fun `shouldIgnoreCaseAndAccents_onBothSides`() {
        assertTrue(LabelSearch.of("peage").matches("Péage autoroute"))
        assertTrue(LabelSearch.of("PÉAGE").matches("peage autoroute"))
        assertTrue(LabelSearch.of("noel").matches("Cadeaux de Noël"))
    }

    @Test
    fun `shouldIgnoreSurroundingSpaces`() {
        assertTrue(LabelSearch.of("  loyer ").matches("Loyer octobre"))
    }

    @Test
    fun `shouldMatchEverything_whenTheFragmentIsBlank`() {
        assertTrue(LabelSearch.of(null).isEmpty)
        assertTrue(LabelSearch.of("   ").isEmpty)
        assertTrue(LabelSearch.of("").matches("Anything"))
    }

    // A space inside the fragment is part of what is searched: "rue de" is not "rue" and "de" anywhere.
    @Test
    fun `shouldMatchTheFragmentAsAWhole`() {
        assertTrue(LabelSearch.of("courses carre").matches("Courses Carrefour"))
        assertFalse(LabelSearch.of("carrefour courses").matches("Courses Carrefour"))
    }
}
