package fr.sacane.jmanager.domain.port.spi

interface InfraTransactionProviderPort {
    companion object{
        val DEFAULT: InfraTransactionProviderPort
            get() = object : InfraTransactionProviderPort {
                override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
                    return executable(input)
                }
            }
    }


    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}

