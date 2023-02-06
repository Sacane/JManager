package fr.sacane.jmanager.domain_server

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.infra.server.entity.UserResource
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserDomainServerTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun deleteUserTest(){
        userRepository.deleteByPseudonym("Sacane_test")
    }

    @Test
    fun `User registered has its password check correctly`(){
        val password = Password("01012000")
        val userEntity = UserResource(null, "Sacane_test", "Sacane", password.get(), "sacane.test@grostest.fr", null, null)
        userRepository.save(userEntity)

        val getUser = userRepository.findByPseudonym("Sacane_test")
        assertThat(getUser).isNotNull
        assertThat(getUser?.username).isEqualTo("Sacane")
        assertThat(getUser?.password).isEqualTo(Password("01012000").get())


    }


}