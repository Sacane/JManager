package fr.sacane.jmanager.domain.models.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag

interface BaseTransaction {
    var label: String
    var amount: Amount
    var isIncome: Boolean
    var tag: Tag?
}