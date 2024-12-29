package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import org.junit.jupiter.api.Assertions.*

fun <T> Result<T>.assertEquals(expectedData: T) {
    assertEquals(ResultState.OK, this.status) { "Expected status SUCCESS but got ${this.status} with message: ${this.message}" }
    this.onSuccess{data -> assertEquals(expectedData, data)}
}
infix fun <T> Result<T>.shouldBe(expectedData: T) {
    assertEquals(ResultState.OK, this.status)
    this.onSuccess{data -> assertEquals(expectedData, data)}
}

fun <T> Result<T>.assertTrue(predicate: T.() -> Boolean){
    assertEquals(ResultState.OK, this.status) { "Expected status SUCCESS but got ${this.status} with message: ${this.message}" }
    this.onSuccess{data ->
        assertTrue(predicate(data))
    }
}

fun <T> Result<T>.assertFailure(resultState: ResultState? = null) {
    if(resultState != null) assertEquals(resultState, this.status)
    assertTrue(this.isFailure())
}

fun <T> Result<T>.assertSuccess() {
    assertTrue(this.isSuccess(), "Expected status SUCCESS but got ${this.status} with message: ${this.message}")
}

fun <T> Result<List<T>>.assertContainsAtPosition(position: Int, expectedData: T): Result<List<T>> {
    assertSuccess()
    onSuccess { data -> assertEquals(expectedData, data[position]) }
    return this
}

fun <T, R> Result<List<T>>.assertEqualsAtPosition(position: Int, expectedResult: R, expectedDataResult: T.() -> R): Result<List<T>> {
    assertSuccess()
    onSuccess { data -> assertEquals(expectedResult, expectedDataResult(data[position])) }
    return this
}

fun <T> T?.asNullableDomainResult(state: ResultState = ResultState.INVALID): Result<T> {
    return if(this == null) Result(state) else success(this)
}