package fr.sacane.jmanager.infra.server.entity

import java.io.Serial
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name="login")
class Login(
    @Id
    @GeneratedValue
    @Column(name = "token")
    var id: UUID?,

    @OneToOne
    @JoinTable(
        name = "token_user",
        joinColumns = [JoinColumn(name = "token_id")],
        inverseJoinColumns = [JoinColumn(name = "id_user")]
    )
    var user: UserResource?,

    @Column(name = "last_refresh")
    var lastRefresh: LocalDateTime?,

    @GeneratedValue
    @Column(name="refresh_token")
    var refreshToken: UUID?

){
    constructor(user: UserResource, lastRefresh: LocalDateTime, refreshToken: UUID?): this(null, user, lastRefresh, refreshToken)
    companion object{
        @Serial
        val serialVersionUID: Long = 423542135L
    }
}