package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractRegularTransactionResource(
    @Column(nullable = false)
    open var label: String,

    @Column(nullable = false)
    open var amount: Double,

    @Column(nullable = false)
    open var isIncome: Boolean,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "tag_id", referencedColumnName = "idTag")
    open val tag: DefaultTagResource? = null,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "personal_tag_id", referencedColumnName = "idTag")
    open val personalTag:TagPersonalResource? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    open val owner: UserResource? = null
) {
    abstract fun toDomain(): RegularTransaction
}
