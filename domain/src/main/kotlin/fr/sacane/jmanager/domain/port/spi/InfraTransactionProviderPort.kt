package fr.sacane.jmanager.domain.port.spi

@Suppress("kotlin:SAM_CONVERTIBLE_INTERFACES_SHOULD_BE_FUN_INTERFACES")
interface InfraTransactionProviderPort {
    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}

