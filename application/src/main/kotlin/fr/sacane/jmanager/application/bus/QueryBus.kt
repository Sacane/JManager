package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Dispatches a [Query] to the appropriate [QueryHandler].
 */
interface QueryBus {
    fun <R> dispatch(query: Query<R>): Result<R>
}

@Component
class SpringQueryBus(handlers: List<QueryHandler<*, *>>) : QueryBus {

    companion object {
        private val log = LoggerFactory.getLogger(SpringQueryBus::class.java)
    }

    private val handlerMap: Map<Class<*>, QueryHandler<*, *>> =
        handlers.associateBy { it.queryClass.java as Class<*> }
            .also { log.info("QueryBus initialized with {} handler(s): {}", it.size, it.keys.map { k -> k.simpleName }) }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(query: Query<R>): Result<R> {
        val handler = handlerMap[query::class.java] as? QueryHandler<Query<R>, R>
            ?: throw IllegalArgumentException("No query handler registered for ${query::class.simpleName}")
        return handler.handle(query)
    }
}
