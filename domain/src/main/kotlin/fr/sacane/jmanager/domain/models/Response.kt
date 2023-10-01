package fr.sacane.jmanager.domain.models

import java.util.function.Consumer

enum class ResponseState{
    OK,
    TIMEOUT,
    INVALID,
    FORBIDDEN,
    NOT_FOUND,
    UNAUTHORIZED;

    fun isSuccess(): Boolean{
        return this == OK
    }
    fun isFailure(): Boolean{
        return this != OK
    }
}

class Response <S> private constructor(
    val status: ResponseState,
    private var value: S? = null,
    private var error: String = "This response is not an error"
){
    val message: String
        get() = error

    companion object{
        fun <S> ok(entity: S): Response<S> = Response(ResponseState.OK, entity)
        fun ok(): Response<Nothing> = Response(ResponseState.OK)
        fun <S> invalid(): Response<S> = Response(ResponseState.INVALID)
        fun <S> timeout(): Response<S> = Response(ResponseState.TIMEOUT)
        fun <S> notFound(): Response<S> = Response(ResponseState.NOT_FOUND)
        fun <S> forbidden(): Response<S> = Response(ResponseState.FORBIDDEN)

        fun <S> notFound(message: String): Response<S> = Response(ResponseState.NOT_FOUND, error=message)
        fun <S> invalid(message: String): Response<S> = Response(ResponseState.INVALID, error=message)
        fun <S> timeout(message: String): Response<S> = Response(ResponseState.TIMEOUT, error=message)
        fun <S> forbidden(message:String): Response<S> = Response(ResponseState.FORBIDDEN, error=message)
        fun <S> unauthorized(message: String): Response<S> = Response(ResponseState.UNAUTHORIZED, error=message)
    }

    fun onSuccess(consumer: Consumer<S>): Response<S> {
        if(this.status.isSuccess()) consumer.accept(this.value!!)
        return this
    }

    fun orElseGet(s: S): S = this.value ?: s
    fun orElse(s: () -> S): S {
        return s.invoke()
    }

    private fun isSuccessAndNotEmpty(): Boolean {
        return isSuccess() && value != null
    }

    fun <T> mapBoth(
        onSuccess: (S?) -> T,
        onFailure: (Pair<String, ResponseState>) -> T
    ): T? = when {
        isSuccess() && value == null -> null
        isSuccessAndNotEmpty() -> onSuccess.invoke(value)
        isFailure() -> onFailure.invoke(Pair(message, status))
        else -> null
    }

    fun isSuccess(): Boolean{
        return this.status.isSuccess()
    }
    fun isFailure(): Boolean{
        return this.status.isFailure()
    }

    fun <T> map(
        mapper: (S) -> T
    ): Response<T> {
        val value = this.value ?: return Response(this.status, null, error = this.error)
        return Response(this.status, mapper.invoke(value))
    }

    fun <T> mapTo (
            mapper: (S?) -> T
    ): T {
        return mapper.invoke(this.value)
    }
}