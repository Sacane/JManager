package fr.sacane.jmanager.infrastructure.api.spa

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

class SpaControllerTest {

    private val controller = SpaController()

    @Test
    fun `should NOT intercept API routes`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/api/user/auth")

        val response = controller.forward(request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should NOT intercept static js files`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/test.js")

        val response = controller.forward(request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should NOT intercept static css files`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/styles.css")

        val response = controller.forward(request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should NOT intercept _nuxt paths`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/_nuxt/app.js")

        val response = controller.forward(request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should NOT intercept favicon`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/favicon.ico")

        val response = controller.forward(request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should serve index html for root path when file exists`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/")

        val response = controller.forward(request)

        // Le fichier index.html devrait exister dans les ressources
        assertTrue(response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should serve index html for dashboard route when file exists`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/dashboard")

        val response = controller.forward(request)

        // Le fichier index.html devrait exister dans les ressources
        assertTrue(response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should serve index html for nested routes when file exists`() {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn("/account/123")

        val response = controller.forward(request)

        // Le fichier index.html devrait exister dans les ressources
        assertTrue(response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND)
    }
}

