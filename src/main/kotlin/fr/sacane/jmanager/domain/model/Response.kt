package fr.sacane.jmanager.domain.model


class Response <out S> private constructor(
    val status: TicketState,
    private val value: S? = null,
){
    init{
        require(status.isSuccess() && value != null || status.isFailure()){
            "Success status should lead to a not null value"
        }
    }
    companion object{
        fun <S> ok(entity: S): Response<S> = Response(TicketState.OK, entity)
        fun <S> invalid(entity: S?): Response<S> = Response(TicketState.INVALID, entity)
        fun invalid(): Response<Nothing> = invalid(null)
        fun <S> timeout(entity: S): Response<S> = Response(TicketState.TIMEOUT, entity)
        fun timeout(): Response<Nothing> = Response(TicketState.TIMEOUT, null)
        fun <S> emptyInvalidResponse(): Response<S> = Response(TicketState.INVALID, null)
    }
    fun get(): S?{
        return value
    }
}