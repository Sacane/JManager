package fr.sacane.jmanager.application.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val caches = listOf(
            buildCache("defaultTag", maximumSize = 1, expireAfterWrite = Duration.ofDays(1)),
            buildCache("allTags", maximumSize = 500, expireAfterWrite = Duration.ofMinutes(30)),
            buildCache("allBooklets", maximumSize = 500, expireAfterWrite = Duration.ofMinutes(30)),
            buildCache("userSettings", maximumSize = 500, expireAfterWrite = Duration.ofMinutes(60)),
        )
        return SimpleCacheManager().apply { setCaches(caches) }
    }

    private fun buildCache(name: String, maximumSize: Long, expireAfterWrite: Duration): CaffeineCache {
        val cache = Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(expireAfterWrite)
            .build<Any, Any>()
        return CaffeineCache(name, cache)
    }
}
