package fr.sacane.jmanager.application.api.setup

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.application.State
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.BookletMapper
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class BookletStateTestAdapter(
    private val bookletJpaRepository: BookletJpaRepository,
    private val bookletMapper: BookletMapper
): State<Booklet, Booklet> {
    @Transactional
    override fun get(): Collection<Booklet> {
        return bookletJpaRepository.findAll().map { it.toModel() }
    }

    override fun clear() {
        bookletJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<Booklet>) {
        bookletJpaRepository.saveAll(initialState.map { bookletMapper.asResource(it) })
    }
}
