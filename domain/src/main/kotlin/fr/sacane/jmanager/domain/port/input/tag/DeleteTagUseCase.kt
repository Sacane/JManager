package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface DeleteTagUseCase {
    fun deleteTag(token: SessionToken, tagId: UUID, force: Boolean): Result<Nothing>
}
