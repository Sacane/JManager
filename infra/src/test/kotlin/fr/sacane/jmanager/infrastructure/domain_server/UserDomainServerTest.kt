package fr.sacane.jmanager.infrastructure.domain_server

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.infrastructure.postgres.entity.UserResource
import fr.sacane.jmanager.infrastructure.postgres.repositories.UserRepository
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
        userRepository.deleteByUsername("Sacane_test")
    }

    @Test
    fun `User registered has its password check correctly`(){
        val pwd1 = Password("01012000")
        userRepository.deleteByUsername("Sacane_test")
        val userEntity = UserResource("Sacane_test", pwd1.get(), "sacane.test@grostest.fr")
        userRepository.save(userEntity)
        val getUser = userRepository.findByUsername("Sacane_test")
        assertThat(getUser).isNotNull
        assertThat(getUser?.username).isEqualTo("Sacane_test")
        assertThat(Hash.contentEquals(getUser?.password!!, "01012000")).isTrue
    }


}