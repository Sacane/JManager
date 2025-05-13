package fr.sacane.jmanager.domain.models.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag

abstract class BaseTransaction {
    abstract var label: String
    abstract var amount: Amount
    abstract var isIncome: Boolean
    open var tag: Tag = Tag("Aucune", isDefault = true)
}