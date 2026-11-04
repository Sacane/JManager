package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.models.BookletMonthlyCycleSetting
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.models.inDisplayOrder

/** The settings of this user, their booklets in the order they chose. */
internal fun User.toSettings(): UserSettings = UserSettings(
    projectionWindowDays = projectionWindowDays,
    bookletCycles = booklets.inDisplayOrder().mapNotNull { booklet ->
        val bookletId = booklet.id ?: return@mapNotNull null
        BookletMonthlyCycleSetting(
            bookletId = bookletId,
            bookletLabel = booklet.label,
            monthlyPeriodStartDay = booklet.monthlyPeriodStartDay,
            monthlyPeriodEndDay = booklet.monthlyPeriodEndDay,
            budgetTarget = booklet.budgetTarget,
        )
    }
)
