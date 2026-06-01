package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Decorator around [SpringQueryBus] that:
 * 1. Injects domain context (bookletId, transactionId) into the SLF4J MDC when the query
 *    implements [MdcContextProvider]. Keys are NOT removed after dispatch — [RequestIdFilter]
 *    calls MDC.clear() at the end of the HTTP request so the full context remains available
 *    to error handlers (e.g. ProblemDetailHandler) if the request fails.
 * 2. Emits one INFO line per dispatch with the query name, result state, and execution time.
 *
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
        putMdcContext(query)
        val start = System.nanoTime()
        val result = delegate.dispatch(query)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("QUERY  | {:<50} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }

    private fun putMdcContext(query: Query<*>) {
        if (query !is MdcContextProvider) return
        query.mdcContext()
            .filterValues { it.isNotBlank() }
            .forEach { (k, v) -> MDC.put(k, v) }
    }
}
