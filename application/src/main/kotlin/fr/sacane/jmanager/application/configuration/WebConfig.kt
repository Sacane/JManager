package fr.sacane.jmanager.application.configuration

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
        registry.addResourceHandler("/_nuxt/**", "/assets/**", "/*.ico", "/*.png", "/*.jpg", "/*.svg")
            .addResourceLocations("classpath:/static/_nuxt/", "classpath:/static/assets/", "classpath:/static/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)))
            .resourceChain(true)

        registry.addResourceHandler("/index.html")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.noCache())
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
            .allowedOrigins("http://localhost:3000", "http://localhost:8060")
            .allowCredentials(true)
    }
}