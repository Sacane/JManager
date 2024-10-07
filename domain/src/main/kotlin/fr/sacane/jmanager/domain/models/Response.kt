package fr.sacane.jmanager.domain.models

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
    private var data: S? = null,
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

    fun onSuccess(consumer: (S) -> Unit): Response<S> {
        if(!this.status.isSuccess()) {
            return this
        }
        if(data == null) {
            return this
        } else {
            consumer(data!!)
        }
        return this
    }

    private fun isSuccessAndNotEmpty(): Boolean {
        return isSuccess() && data != null
    }

    fun <T> mapBoth(
        onSuccess: (S?) -> T,
        onFailure: (Pair<String, ResponseState>) -> T
    ): T? = when {
        isSuccess() && data == null -> null
        isSuccessAndNotEmpty() -> onSuccess.invoke(data)
        isFailure() -> onFailure(Pair(message, status))
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
        val value = this.data ?: return Response(this.status, null, error = this.error)
        return Response(this.status, mapper.invoke(value))
    }

    fun <T> mapTo (
            mapper: (S?) -> T
    ): T {
        return mapper.invoke(this.data)
    }
}