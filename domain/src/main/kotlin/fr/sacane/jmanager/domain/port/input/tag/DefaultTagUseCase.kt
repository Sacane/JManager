package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DefaultTagUseCase {
    fun handle(query: DefaultTagQuery): Result<Tag>
}
