package fr.sacane.jmanager.domain.port.spi

interface UnitOfWorkTransactionProviderPort {
    companion object{
        val DEFAULT: UnitOfWorkTransactionProviderPort
            get() = object : UnitOfWorkTransactionProviderPort {
                override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
                    return executable(input)
                }
            }
    }


    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}

