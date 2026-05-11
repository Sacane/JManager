package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.User
import java.util.UUID

object BookletFixture {

    fun aBooklet(
        id: UUID? = UUID.randomUUID(),
        label: String = "Main booklet",
        amount: Amount = Amount(0),
        owner: User? = UserFixture.aUser()
    ) = Booklet(amount = amount, label = label, owner = owner, id = id)
}
