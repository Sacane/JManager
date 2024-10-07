package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.ResponseState
import org.junit.jupiter.api.Assertions.*

fun <T> Response<T>.assertEquals(expectedData: T) {
    assertEquals(ResponseState.OK, this.status) { "Expected status SUCCESS but got ${this.status} with message: ${this.message}" }
    this.onSuccess{data -> assertEquals(expectedData, data)}
}

fun <T> Response<T>.assertTrue(predicate: T.() -> Boolean){
    assertEquals(ResponseState.OK, this.status) { "Expected status SUCCESS but got ${this.status} with message: ${this.message}" }
    this.onSuccess{data ->
        assertTrue(predicate(data))
    }
}

fun <T> Response<T>.assertFailure(responseState: ResponseState? = null) {
    if(responseState != null) assertEquals(responseState, this.status)
    assertTrue(this.isFailure())
}

fun <T> Response<T>.assertSuccess() {
    assertTrue(this.isSuccess())
}

fun <T> Response<List<T>>.assertContainsAtPosition(position: Int, expectedData: T): Response<List<T>> {
    assertSuccess()
    onSuccess { data -> assertEquals(expectedData, data[position]) }
    return this
}

fun <T> T?.asResponse(): Response<T>{
    return if(this == null) Response.invalid() else ok(this)
}