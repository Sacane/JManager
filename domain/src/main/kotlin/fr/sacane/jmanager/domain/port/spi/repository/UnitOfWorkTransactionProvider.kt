package fr.sacane.jmanager.domain.port.spi.repository

interface UnitOfWorkTransactionProvider {
    companion object{
        val DEFAULT: UnitOfWorkTransactionProvider
            get() = object : UnitOfWorkTransactionProvider {
                override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
                    return executable(input)
                }
            }
    }


    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}

