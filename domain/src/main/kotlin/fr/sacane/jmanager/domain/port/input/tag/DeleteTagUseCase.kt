package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DeleteTagUseCase {
    fun handle(command: DeleteTagCommand): Result<Nothing>
}
