package fr.sacane.jmanager.application.api.spa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Which paths reach the controller is decided by its mapping, not by the method: that is covered against a
 * running server in [SpaRoutingTest]. Calling `forward()` four times with different test names, as this
 * class used to, could not detect a page missing from the mapping — which is how several went unnoticed.
 */
class SpaControllerTest {

    @Test
    fun `forwards to the application shell`() {
        assertEquals("forward:/index.html", SpaController().forward())
    }
}
