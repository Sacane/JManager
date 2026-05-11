package fr.sacane.jmanager.domain

/**
 * Marks the precondition setup phase of a test scenario (Arrange).
 * Returns the value produced by [block] for use in subsequent phases.
 */
inline fun <T> given(block: () -> T): T = block()

/**
 * Marks the action under test (Act).
 * Returns the result produced by [block].
 */
inline fun <T> act(block: () -> T): T = block()

/**
 * Marks the assertion phase (Assert).
 */
inline fun <T> then(result: T, block: T.() -> Unit) = result.block()
