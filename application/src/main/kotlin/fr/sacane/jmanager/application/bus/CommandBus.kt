package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Dispatches a [Command] to the appropriate [CommandHandler].
 */
interface CommandBus {
    fun <R> dispatch(command: Command<R>): Result<R>
}

@Component
class SpringCommandBus(handlers: List<CommandHandler<*, *>>) : CommandBus {

    companion object {
        private val log = LoggerFactory.getLogger(SpringCommandBus::class.java)
    }

    private val handlerMap: Map<Class<*>, CommandHandler<*, *>> =
        handlers.associateBy { it.commandClass.java as Class<*> }
            .also { log.info("CommandBus initialized with {} handler(s): {}", it.size, it.keys.map { k -> k.simpleName }) }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(command: Command<R>): Result<R> {
        val handler = handlerMap[command::class.java] as? CommandHandler<Command<R>, R>
            ?: throw IllegalArgumentException("No command handler registered for ${command::class.simpleName}")
        return handler.handle(command)
    }
}
