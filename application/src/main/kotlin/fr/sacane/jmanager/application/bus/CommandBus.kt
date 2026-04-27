package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Dispatches a [Command] to the appropriate [CommandHandler].
 */
interface CommandBus {
    fun <R> dispatch(command: Command<R>): Result<R>
}

@Component
class SpringCommandBus(handlers: List<CommandHandler<*, *>>) : CommandBus {

    companion object {
        private val LOGGER = Logger.getLogger(SpringCommandBus::class.java.name)
    }

    private val handlerMap: Map<Class<*>, CommandHandler<*, *>> =
        handlers.associateBy { it.commandClass.java as Class<*> }
            .also { LOGGER.info("CommandBus initialized with ${it.size} handler(s): ${it.keys.map { k -> k.simpleName }}") }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(command: Command<R>): Result<R> {
        val handler = handlerMap[command::class.java] as? CommandHandler<Command<R>, R>
            ?: throw IllegalArgumentException("No command handler registered for ${command::class.simpleName}")
        return handler.handle(command)
    }
}
