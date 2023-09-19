package fr.sacane.jmanager.infrastructure.postgres.entity

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
    var id: UUID? = null,
    @GeneratedValue
    @Column(name="token")
    var token: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinTable(
        name = "token_user",
        joinColumns = [JoinColumn(name = "id_token")],
        inverseJoinColumns = [JoinColumn(name = "id_user")]
    )
    var user: UserResource? = null,
    @Column(name = "token_lifetime")
    var tokenLifeTime: LocalDateTime = LocalDateTime.now(),
    @GeneratedValue
    @Column(name="refresh_token")
    var refreshToken: UUID = UUID.randomUUID(),

    @Column(name="refresh_token_lifetime")
    var refreshTokenLifetime: LocalDateTime = LocalDateTime.now()

){
    companion object{
        @Serial
        val serialVersionUID: Long = 423542135L
    }
}