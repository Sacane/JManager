package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Page
import kotlin.math.min

sealed interface Paginator {
    fun <T> paginate(pageNumber: Int, pageSize: Int, items: () -> List<T>): Page<T>
}

@DomainService
class PaginatorImpl: Paginator {
    override fun <T> paginate(pageNumber: Int, pageSize: Int, items: () -> List<T>): Page<T> {
        val normalizedPageSize = if (pageSize <= 0) 20 else pageSize
        val normalizedPageNumber = if (pageNumber < 0) 0 else pageNumber
        val items = items()
        val totalElements = items.size.toLong()
        val fromIndex = normalizedPageNumber.toLong() * normalizedPageSize.toLong()

        val content: List<T> = if (fromIndex >= totalElements) {
            emptyList()
        } else {
            val toIndexExclusive = min(totalElements, fromIndex + normalizedPageSize)
            items.subList(fromIndex.toInt(), toIndexExclusive.toInt())
        }

        val totalPages = ((totalElements + normalizedPageSize.toLong() - 1L) / normalizedPageSize.toLong()).toInt()

        return Page(
            content = content,
            pageNumber = normalizedPageNumber,
            pageSize = normalizedPageSize,
            totalElements = totalElements,
            totalPages = totalPages
        )
    }
}