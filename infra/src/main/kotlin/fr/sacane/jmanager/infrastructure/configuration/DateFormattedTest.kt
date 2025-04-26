package fr.sacane.jmanager.infrastructure.configuration

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun formattedDate(date: LocalDate): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
    return LocalDate.parse(date.format(formatter), formatter)
}