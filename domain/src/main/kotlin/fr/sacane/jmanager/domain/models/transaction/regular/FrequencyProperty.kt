package fr.sacane.jmanager.domain.models.transaction.regular

import java.time.LocalDate

sealed interface FrequencyProperty {
    class Forever: FrequencyProperty

    class SpecificRepetitionTimes(val number: Int): FrequencyProperty

    class UntilDate(val date: LocalDate): FrequencyProperty
}

