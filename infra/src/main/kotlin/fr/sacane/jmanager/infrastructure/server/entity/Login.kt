package fr.sacane.jmanager.infrastructure.server.entity

import jakarta.persistence.*
import java.io.Serial
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name="login")
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
    constructor(): this(null, null, null, null, null)
    constructor(user: UserResource, lastRefresh: LocalDateTime): this(null, UUID.randomUUID(), user, lastRefresh, UUID.randomUUID())
    companion object{
        @Serial
        val serialVersionUID: Long = 423542135L
    }
}