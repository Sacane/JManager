package fr.sacane.jmanager.domain.models.transaction.regular

enum class MonthlyRepeatAttribute(val repeatDay: Int) {
    FIRST_OF_MONTH(1),
    SECOND_OF_MONTH(2),
    THIRD_OF_MONTH(3),
    FOURTH_OF_MONTH(4),
    LAST_OF_MONTH(31)
}
