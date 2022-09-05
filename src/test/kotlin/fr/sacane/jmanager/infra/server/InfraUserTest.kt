package fr.sacane.jmanager.infra.server

import fr.sacane.jmanager.infra.server.entity.UserResource
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class InfraUserTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `users should correctly be implement into postgres database`(){
        userRepository.deleteAll()
        var user = UserResource()
        user.username = "johan"
        user.email = "johan.ramaroson@gmail.com"
        user.password = "01012000"
        user.pseudonym = "Sacane"
        userRepository.save(user)
        userRepository.deleteAll()
    }

}
