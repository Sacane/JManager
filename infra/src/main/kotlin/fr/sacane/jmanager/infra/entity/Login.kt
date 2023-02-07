package fr.sacane.jmanager.infra.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import java.io.Serial
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name="login")
@DatasourceEntity
class Login(
    @Id
    @GeneratedValue
    @Column(name = "id_token")
    var id: UUID?,

    @GeneratedValue
    @Column(name="token")
    var token: UUID?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinTable(
        name = "token_user",
        joinColumns = [JoinColumn(name = "id_token")],
        inverseJoinColumns = [JoinColumn(name = "id_user")]
    )
    var user: UserResource?,

    @Column(name = "last_refresh")
    var lastRefresh: LocalDateTime?,

    @GeneratedValue
    @Column(name="refresh_token")
    var refreshToken: UUID?

){
    constructor(user: UserResource, lastRefresh: LocalDateTime): this(null, null, user, lastRefresh, null)
    companion object{
        @Serial
        val serialVersionUID: Long = 423542135L
    }
}