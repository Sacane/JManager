package fr.sacane.jmanager.domain.port.input

import fr.sacane.jmanager.domain.utils.Result
import kotlin.reflect.KClass

/**
 * Marker interface for command objects (write operations).
 * @param R the result type produced by the handler.
 */
interface Command<R>

/**
 * Handler contract for a command.
 * Each UseCase performing a write operation extends this interface.
 */
interface CommandHandler<C : Command<R>, R> {
    /** The concrete [Command] type this handler is responsible for. */
    val commandClass: KClass<C>
    fun handle(command: C): Result<R>
}
