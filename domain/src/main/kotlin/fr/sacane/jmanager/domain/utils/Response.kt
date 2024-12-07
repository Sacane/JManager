package fr.sacane.jmanager.domain.utils

enum class ResponseState (code: Int){
    OK(0), TIMEOUT(1), INVALID(2), FORBIDDEN(3), NOT_FOUND(4), UNAUTHORIZED(5),
    INTERNAL_SERVER_ERROR(6), BAD_REQUEST(7),
    // DOMAIN ERROR
    BOOKLET_NOT_FOUND(1001),
    TRANSACTION_NOT_FOUND(1002),
    TAG_NOT_FOUND(1003),
    USER_NOT_FOUND(1004),
    BOOKLET_LABEL_NOT_EXIST(1005),
    TAG_PLACEHOLDER_UNDEFINED(1006),

    BOOKLET_LABEL_EXIST(2001),
    TAG_LABEL_ALREADY_TAKEN(2002),

    USER_NOT_AUTHENTICATED(3001),
    USER_UNAUTHORIZED(3002),

    TRANSACTION_ENTRY_ERROR(3003),
    REGISTRATION_ERROR(5001)
    ;

    fun isSuccess(): Boolean = this == OK
    fun isFailure(): Boolean = !isSuccess()
}

class Response <S> internal constructor(
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