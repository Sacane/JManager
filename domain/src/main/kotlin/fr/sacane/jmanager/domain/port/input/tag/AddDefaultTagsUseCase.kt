package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side

@Port(Side.APPLICATION)
interface AddDefaultTagsUseCase {
    fun addDefaultTags()
}
