package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Decorator around [SpringCommandBus] that:
 * 1. Injects domain context (bookletId, transactionId) into the SLF4J MDC when the command
 *    implements [MdcContextProvider]. Keys are NOT removed after dispatch — [RequestIdFilter]
 *    calls MDC.clear() at the end of the HTTP request so the full context remains available
 *    to error handlers (e.g. ProblemDetailHandler) if the request fails.
 * 2. Emits one INFO line per dispatch with the command name, result state, and execution time.
 *
 * ```
 * COMMAND | SaveBookletCommand              | OK               |  12 ms
 * COMMAND | DeleteBookletByIdCommand        | BOOKLET_NOT_FOUND |   3 ms
 * ```
 *
 * No payload is logged — command fields may contain user data.
 */
@Component
class LoggingCommandBus(
    private val delegate: SpringCommandBus
) : CommandBus {

    private val log = LoggerFactory.getLogger(LoggingCommandBus::class.java)

    override fun <R> dispatch(command: Command<R>): Result<R> {
        val name = command::class.simpleName ?: "UnknownCommand"
        putMdcContext(command)
        val start = System.nanoTime()
        val result = delegate.dispatch(command)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("COMMAND | {:<50} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }

    private fun putMdcContext(command: Command<*>) {
        if (command !is MdcContextProvider) return
        command.mdcContext()
            .filterValues { it.isNotBlank() }
            .forEach { (k, v) -> MDC.put(k, v) }
    }
}
