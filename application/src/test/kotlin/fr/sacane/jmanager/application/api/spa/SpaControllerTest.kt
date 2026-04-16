package fr.sacane.jmanager.application.api.spa

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpaControllerTest {

    private val controller = SpaController()

    @Test
    fun `should forward root path to index html`() {
        val result = controller.forward()
        assertEquals("forward:/index.html", result)
    }

    @Test
    fun `should forward dashboard route to index html`() {
        val result = controller.forward()
        assertEquals("forward:/index.html", result)
    }

    @Test
    fun `should forward booklet route to index html`() {
        val result = controller.forward()
        assertEquals("forward:/index.html", result)
    }

    @Test
    fun `should forward nested routes to index html`() {
        val result = controller.forward()
        assertEquals("forward:/index.html", result)
    }
}
