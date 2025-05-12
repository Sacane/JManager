package fr.sacane.jmanager.domain.utils

enum class ResultState (val code: Int){
    OK(0), TIMEOUT(1), INVALID(2), FORBIDDEN(3), NOT_FOUND(4), UNAUTHORIZED(5),
    INTERNAL_SERVER_ERROR(6), BAD_REQUEST(7), INFRASTRUCTURE_ERROR(8),
    // DOMAIN ERROR
    BOOKLET_NOT_FOUND(1001),
    TRANSACTION_NOT_FOUND(1002),
    TAG_NOT_FOUND(1003),
    USER_NOT_FOUND(1004),
    BOOKLET_LABEL_NOT_EXIST(1005),
    TAG_PLACEHOLDER_UNDEFINED(1006),

    BOOKLET_LABEL_EXIST(2001),
    TAG_LABEL_ALREADY_TAKEN(2002),
    TAG_SHOULD_NOT_BE_DEFAULT(2003),

    USER_NOT_AUTHENTICATED(3001),
    USER_UNAUTHORIZED(3002),
    PASSWORD_NOT_MATCH(3003),

    TRANSACTION_ENTRY_ERROR(5000),
    REGISTRATION_ERROR(5001);

    fun isSuccess(): Boolean = this == OK
    fun isFailure(): Boolean = !isSuccess()
}

class Result <S>(
    val status: ResultState,
    private var data: S? = null,
    private var error: String = "This response is not an error"
){
    val message: String
        get() = error

    companion object{
        fun <S> unauthorized(message: String): Result<S> =
            Result(ResultState.UNAUTHORIZED, error=message)
    }

    fun onSuccess(consumer: (S) -> Unit): Result<S> {
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
        onFailure: (Pair<String, ResultState>) -> T
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
    ): Result<T> {
        val value = this.data ?: return Result(status, null, error = error)
        return Result(status, mapper.invoke(value))
    }

    fun <T> mapTo (
            mapper: (S?) -> T
    ): T {
        return mapper.invoke(this.data)
    }
}
fun <S> success(entity: S): Result<S> =
    Result(ResultState.OK, entity)
fun success(): Result<Nothing> = Result(ResultState.OK)
fun <S> invalid(): Result<S> = Result(ResultState.INVALID)


fun <S> notFound(message: String): Result<S> =
    Result(ResultState.NOT_FOUND, error=message)
fun <S> invalid(message: String): Result<S> =
    Result(ResultState.INVALID, error=message)

fun <S> failure(state: ResultState, message: String): Result<S> =
    Result(state, error = message)

fun <S> forbidden(message: String): Result<S> =
    Result(ResultState.FORBIDDEN, error=message)

fun <S> timeout(message: String): Result<S> =
    Result(ResultState.TIMEOUT, error=message)