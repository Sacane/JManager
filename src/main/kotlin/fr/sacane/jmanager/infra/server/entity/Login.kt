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
    @Column(name = "login")
    var id: UUID?,

    @OneToOne
    @JoinTable(
        name = "token_user",
        joinColumns = [JoinColumn(name = "token_id")],
        inverseJoinColumns = [JoinColumn(name = "id_user")]
    )
    var user: UserResource?,

    @Column(name = "last_refresh")
    var lastRefresh: LocalDateTime?

){
    constructor(user: UserResource, lastRefresh: LocalDateTime): this(null, user, lastRefresh)
    companion object{
        @Serial
        val serialVersionUID: Long = 423542135L
    }
}