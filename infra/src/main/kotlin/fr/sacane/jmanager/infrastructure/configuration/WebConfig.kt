package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.CacheControl
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration


@Configuration
@EnableScheduling
class WebConfig: WebMvcConfigurer{

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Ressources statiques avec priorité haute
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)))
            .resourceChain(true)
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