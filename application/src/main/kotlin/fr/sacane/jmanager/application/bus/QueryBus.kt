package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Dispatches a [Query] to the appropriate [QueryHandler].
 */
interface QueryBus {
    fun <R> dispatch(query: Query<R>): Result<R>
}

@Component
class SpringQueryBus(handlers: List<QueryHandler<*, *>>) : QueryBus {

    companion object {
        private val LOGGER = Logger.getLogger(SpringQueryBus::class.java.name)
    }

    private val handlerMap: Map<Class<*>, QueryHandler<*, *>> =
        handlers.associateBy { it.queryClass.java as Class<*> }
            .also { LOGGER.info("QueryBus initialized with ${it.size} handler(s): ${it.keys.map { k -> k.simpleName }}") }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(query: Query<R>): Result<R> {
        val handler = handlerMap[query::class.java] as? QueryHandler<Query<R>, R>
            ?: throw IllegalArgumentException("No query handler registered for ${query::class.simpleName}")
        return handler.handle(query)
    }
}
