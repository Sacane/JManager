package fr.sacane.jmanager.domain.port.spi.repository

/**
 * SPI contract that provides a unit-of-work / transaction boundary for coordinating multiple
 * repository operations within a single atomic block.
 *
 * The domain uses this SPI when a use-case must perform several repository operations
 * atomically. Implementations may delegate to a database transaction manager or other
 * transactional infrastructure.
 */
interface UnitOfWorkTransactionProvider {
    companion object{
        val DEFAULT: UnitOfWorkTransactionProvider
            get() = object : UnitOfWorkTransactionProvider {
                override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
                    return executable(input)
                }
            }
    }

    /**
     * Execute the provided function inside a transactional/unit-of-work boundary.
     *
     * Implementations should start a transaction, execute the function and commit/rollback
     * according to the outcome. The input parameter is forwarded to the executable for convenience
     * and can be used to pass context objects.
     *
     * @param input Arbitrary input/context object provided to the executable block.
     * @param executable A function executed inside the transaction context that returns a result.
     * @return The result produced by the executable block.
     */
    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}
