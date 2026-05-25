package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Decorator around [SpringCommandBus] that logs every command dispatch.
 *
 * Emits one INFO line per dispatch:
 * ```
 * COMMAND | SaveBookletCommand              | OK               |  12 ms
 * COMMAND | DeleteBookletByIdCommand        | BOOKLET_NOT_FOUND |   3 ms
 * ```
 *
 * No payload is logged — command fields may contain user data.
 */
@Primary
@Component
class LoggingCommandBus(
    private val delegate: SpringCommandBus
) : CommandBus {

    private val log = LoggerFactory.getLogger(LoggingCommandBus::class.java)

    override fun <R> dispatch(command: Command<R>): Result<R> {
        val name = command::class.simpleName ?: "UnknownCommand"
        val start = System.nanoTime()
        val result = delegate.dispatch(command)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("COMMAND | {:<50} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }
}
