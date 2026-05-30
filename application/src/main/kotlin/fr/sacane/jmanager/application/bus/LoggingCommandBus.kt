package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Decorator around [SpringCommandBus] that:
 * 1. Injects domain context (bookletId, transactionId) into the SLF4J MDC for the duration
 *    of the dispatch when the command implements [MdcContextProvider].
 * 2. Emits one INFO line per dispatch with the command name, result state, and execution time.
 *
 * ```
 * COMMAND | SaveBookletCommand              | OK               |  12 ms
 * COMMAND | DeleteBookletByIdCommand        | BOOKLET_NOT_FOUND |   3 ms
 * ```
 *
 * MDC is cleared in a `finally` block — no context leaks across requests.
 * No payload is logged — command fields may contain user data.
 */
@Component
class LoggingCommandBus(
    private val delegate: SpringCommandBus
) : CommandBus {

    private val log = LoggerFactory.getLogger(LoggingCommandBus::class.java)

    override fun <R> dispatch(command: Command<R>): Result<R> {
        val name = command::class.simpleName ?: "UnknownCommand"
        val mdcKeys = putMdcContext(command)
        val start = System.nanoTime()
        return try {
            val result = delegate.dispatch(command)
            val ms = (System.nanoTime() - start) / 1_000_000
            log.info("COMMAND | {:<50} | {:<30} | {} ms", name, result.status.name, ms)
            result
        } finally {
            mdcKeys.forEach { MDC.remove(it) }
        }
    }

    private fun putMdcContext(command: Command<*>): Set<String> {
        if (command !is MdcContextProvider) return emptySet()
        return command.mdcContext()
            .filterValues { it.isNotBlank() }
            .also { ctx -> ctx.forEach { (k, v) -> MDC.put(k, v) } }
            .keys
    }
}
