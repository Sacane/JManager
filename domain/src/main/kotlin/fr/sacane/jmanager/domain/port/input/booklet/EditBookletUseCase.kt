package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

data class EditBookletCommand(
    val booklet: Booklet,
    val userId: UserId
) : Command<Booklet>, MdcContextProvider {
    override fun mdcContext() = buildMap {
        booklet.id?.let { put(MdcKeys.BOOKLET_ID, it.toString()) }
    }
}

@Port(Side.APPLICATION)
interface EditBookletUseCase : CommandHandler<EditBookletCommand, Booklet> {
    override val commandClass get() = EditBookletCommand::class
}

@DomainService
class EditBookletService(
    private val bookletRepository: BookletRepository
) : EditBookletUseCase {
    override fun handle(command: EditBookletCommand): Result<Booklet> {
        val userId = command.userId
        val booklet = command.booklet
        val bookletID = booklet.id ?: return bookletDomainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le livret ${booklet.label} est introuvable en base",
            "domain.booklet.edit.id_missing"
        )
        if (!userOwnsBooklet(bookletRepository, userId, bookletID)) {
            return bookletDomainFailure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce livret",
                "domain.booklet.edit.forbidden"
            )
        }
        val oldBooklet = bookletRepository.findBookletByIdWithTransactions(bookletID)
            ?: return bookletDomainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret ${booklet.id} est introuvable",
                "domain.booklet.edit.not_found"
            )
        if(oldBooklet.id != booklet.id && oldBooklet.label == booklet.label){
            return bookletDomainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le libellé du livret existe déjà",
                "domain.booklet.edit.label_already_exists"
            )
        }
        oldBooklet.updateFrom(booklet)
        val registered = bookletRepository.upsert(oldBooklet)
        return success(registered)
    }
}
