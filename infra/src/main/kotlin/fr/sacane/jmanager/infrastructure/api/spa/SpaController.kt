package fr.sacane.jmanager.infrastructure.api.spa

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

/**
 * Contrôleur SPA pour gérer le routage client-side de Nuxt.
 *
 * Ce contrôleur redirige toutes les requêtes non-API vers le fichier index.html
 * pour permettre au router Vue de gérer la navigation côté client.
 *
 * SÉCURITÉ :
 * - N'interfère PAS avec les routes /api qui sont protégées par Spring Security
 * - N'intercepte que les routes destinées au front-end
 * - Maintient toutes les règles de sécurité définies dans SecurityConfig
 */
@RestController
class SpaController {

    // Mapping to root and any path. Do not use patterns like "/**/{...}" which are invalid.
    @GetMapping(value = ["/", "/**"])
    fun forward(request: HttpServletRequest): ResponseEntity<Resource> {
        val uri = request.requestURI

        // NEW: Ne pas intercepter les routes API -> laisse Spring gérer les endpoints API
        if (uri.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }

        // Skip files (contain a dot), let static resource handling serve them
        if (uri.contains('.')) {
            return ResponseEntity.notFound().build()
        }

        val index = ClassPathResource("static/index.html")
        return if (index.exists()) {
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(index)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
