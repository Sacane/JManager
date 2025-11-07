package fr.sacane.jmanager.infrastructure.api.spa

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView

@Configuration
class SpaConfiguration {

    @Bean
    fun spaErrorViewResolver(): ErrorViewResolver {
        return ErrorViewResolver { _, status, _ ->
            if (status == HttpStatus.NOT_FOUND) {
                val indexResource = ClassPathResource("static/index.html")
                if (indexResource.exists()) {
                    ModelAndView("forward:/index.html")
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}

@Controller
class SpaController {

    @GetMapping(value = ["/", "/dashboard", "/dashboard/**", "/account", "/account/**",
                          "/login", "/admin", "/admin/**", "/tag", "/tag/**",
                          "/user", "/user/**", "/regular-transaction", "/regular-transaction/**"])
    fun forward(): String {
        return "forward:/index.html"
    }
}
