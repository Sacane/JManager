package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.http.CacheControl
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.RequestPredicates.path
import org.springframework.web.servlet.function.RequestPredicates.pathExtension
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions.route
import org.springframework.web.servlet.function.ServerResponse
import java.time.Duration


@Configuration
@EnableScheduling
class WebConfig: WebMvcConfigurer{

    @Bean
    fun spaRouter(): RouterFunction<ServerResponse> {
        val index = ClassPathResource("static/index.html")
        val spaPredicate = { request: org.springframework.web.servlet.function.ServerRequest ->
            val path = request.path()
            !path.startsWith("/api") &&
                    path != "/error" &&
                    !path.substringAfterLast("/").contains(".")
        }
        return route()
            .GET("/{path:^(?!api|error).*$}", spaPredicate) { ServerResponse.ok().body(index) }
            .build()
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/resources/**", "/static/**")
            .addResourceLocations("/public", "classpath:/static/**")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)))
    }
}

@Configuration
@Profile("local")
class CorsConfig: WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .exposedHeaders("*")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowedOrigins("http://localhost:3000")
            .allowCredentials(true)
    }
}