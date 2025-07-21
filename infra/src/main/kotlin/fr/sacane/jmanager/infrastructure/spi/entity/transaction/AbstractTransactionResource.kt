package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractTransactionResource(
    @Column(nullable = false)
    open var label: String,

    @Column(nullable = false)
    open var amount: Double, // Adapter si Amount est un value object complexe

    @Column(nullable = false)
    open var isIncome: Boolean,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "tag_id", referencedColumnName = "idTag")
    var tag: DefaultTagResource? = null,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "personal_tag_id", referencedColumnName = "idTag")
    var personalTag:TagPersonalResource? = null,
)
