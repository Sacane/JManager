package fr.sacane.jmanager.domain.models

import java.util.function.Consumer
import java.util.function.Supplier

enum class ResponseState{
    OK,
    TIMEOUT,
    INVALID,
    NOT_FOUND;
    fun isSuccess(): Boolean{
        return this == OK
    }
    fun isFailure(): Boolean{
        return this != OK
    }
}

data class Error(
    val code: ResponseState,
    val message: String
){
    init {
        check(code.isFailure()) {
            "Error code must be a failure"
        }
    }
}

class Response <S> private constructor(
    val status: ResponseState,
    private var value: S? = null,
    private var error: Error? = null
){
    companion object{
        fun <S> ok(entity: S): Response<S> = Response(ResponseState.OK, entity)
        fun ok(): Response<Nothing> = Response(ResponseState.OK, null)
        fun <S> invalid(): Response<S> = Response(ResponseState.INVALID, null)
        fun <S> timeout(): Response<S> = Response(ResponseState.TIMEOUT, null)
        fun <S> notFound(): Response<S> = Response(ResponseState.NOT_FOUND, null)
    }

    fun onSuccess(consumer: Consumer<S>): Response<S> {
        if(this.status.isSuccess()) consumer.accept(this.value!!)
        return this
    }

    fun orElseGet(s: S): S? {
        if(this.status.isFailure()) return s
        return this.value
    }

    fun onError(consumer: (Error) -> Unit) {
        if(this.status.isSuccess()) return;
        consumer.invoke(this.error!!)
    }

    fun orElse(s: () -> S): S {
        return s.invoke()
    }

    fun isSuccess(): Boolean{
        return this.status.isSuccess()
    }
    fun isFailure(): Boolean{
        return this.status.isFailure()
    }

    fun <T> map(
        mapper: (S?) -> T
    ): Response<T> {
        val mapped = mapper.invoke(this.value)
        return Response(this.status, mapped)
    }

    fun <T> mapTo (
            mapper: (S?) -> T
    ): T {
        return mapper.invoke(this.value)
    }
}