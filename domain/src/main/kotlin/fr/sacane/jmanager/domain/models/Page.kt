package fr.sacane.jmanager.domain.models

data class Page<T>(
    val content: List<T> = emptyList(),
    val pageNumber: Int = 0,
    val pageSize: Int = 0,
    val totalElements: Long = 0L,
    val totalPages: Int = 0
)