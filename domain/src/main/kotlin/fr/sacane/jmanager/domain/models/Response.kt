package fr.sacane.jmanager.domain.models

enum class ResponseState{
    OK,
    TIMEOUT,
    INVALID;
    fun isSuccess(): Boolean{
        return this == OK
    }
    fun isFailure(): Boolean{
        return this != OK
    }
}
class Response <out S> private constructor(
    val status: ResponseState,
    private var value: S? = null,
){
    init{
        require(status.isSuccess() && value != null || status.isFailure()){
            "Success status should lead to a not null value"
        }
    }
    companion object{
        fun <S> ok(entity: S): Response<S> = Response(ResponseState.OK, entity)
        fun <S> invalid(entity: S?): Response<S> = Response(ResponseState.INVALID, entity)
        fun invalid(): Response<Nothing> = invalid(null)
        fun <S> timeout(entity: S): Response<S> = Response(ResponseState.TIMEOUT, entity)
        fun timeout(): Response<Nothing> = Response(ResponseState.TIMEOUT, null)
        fun <S> emptyInvalidResponse(): Response<S> = Response(ResponseState.INVALID, null)
    }
    fun get(): S?{
        return value
    }
    fun <T> mapTo(
        mapper: (S?) -> T
    ): Response<T> {
        val mapped = mapper.invoke(this.value)
        return Response(this.status, mapped)
    }
}