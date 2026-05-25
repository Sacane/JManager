package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Decorator around [SpringQueryBus] that logs every query dispatch.
 *
 * Emits one INFO line per dispatch:
 * ```
 * QUERY  | FindAllRegisteredBookletsQuery  | OK               |   8 ms
 * QUERY  | FindBookletByIdQuery            | NOT_FOUND        |   2 ms
 * ```
 *
 * No payload is logged — query fields may contain user data.
 */
@Primary
@Component
class LoggingQueryBus(
    private val delegate: SpringQueryBus
) : QueryBus {

    private val log = LoggerFactory.getLogger(LoggingQueryBus::class.java)

    override fun <R> dispatch(query: Query<R>): Result<R> {
        val name = query::class.simpleName ?: "UnknownQuery"
        val start = System.nanoTime()
        val result = delegate.dispatch(query)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("QUERY  | {:<50} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }
}
