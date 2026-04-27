package fr.sacane.jmanager.domain.port.input

import fr.sacane.jmanager.domain.utils.Result
import kotlin.reflect.KClass

/**
 * Marker interface for query objects (read operations).
 * @param R the result type produced by the handler.
 */
interface Query<R>

/**
 * Handler contract for a query.
 * Each UseCase performing a read operation extends this interface.
 */
interface QueryHandler<Q : Query<R>, R> {
    /** The concrete [Query] type this handler is responsible for. */
    val queryClass: KClass<Q>
    fun handle(query: Q): Result<R>
}
