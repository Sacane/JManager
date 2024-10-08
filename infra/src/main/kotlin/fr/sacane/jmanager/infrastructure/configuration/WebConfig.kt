package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration


@Configuration
@EnableScheduling
class WebConfig: WebMvcConfigurer{


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("Authorization", "X-Requested-With", "content-type")
            .exposedHeaders("Authorization")
    }
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/resources/**", "/static/**")
            .addResourceLocations("/public", "classpath:/static/**")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)))
    }
}