package fr.sacane.jmanager.domain.port.input

/**
 * Implemented by [Command] and [Query] objects that carry identifiable domain context
 * (booklet ID, transaction ID, etc.).
 *
 * The bus infrastructure reads [mdcContext] before dispatch and injects the entries into
 * the SLF4J MDC so that every log line produced during the operation includes them.
 * The MDC is cleared in a `finally` block after dispatch — the domain never touches MDC directly.
 */
interface MdcContextProvider {
    /**
     * Returns the key-value pairs to inject into the MDC for the duration of this dispatch.
     * Use [MdcKeys] constants as keys for consistency.
     * Empty entries (blank values) are ignored by the bus.
     */
    fun mdcContext(): Map<String, String>
}

/**
 * Canonical MDC key names shared by all commands, queries, and the Logback pattern.
 */
object MdcKeys {
    const val BOOKLET_ID = "bookletId"
    const val TRANSACTION_ID = "transactionId"
}
