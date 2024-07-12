package fr.sacane.jmanager.domain.port.spi

interface InfraTransactionProviderPort {
    fun <T, R> executeInTransaction(input: T, executable: (T) -> R) : R
}