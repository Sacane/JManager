package fr.sacane.jmanager.infrastructure.domain_server

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class UserDomainServerTest {

    @Autowired
    private lateinit var userPostgresRepository: UserPostgresRepository

    @AfterEach
    fun deleteUserTest(){
        userPostgresRepository.deleteByUsername("Sacane_test")
    }

    @Test
    fun `User registered has its password check correctly`(){
        val pwd1 = Password("01012000")
        userPostgresRepository.deleteByUsername("Sacane_test")
        val userEntity = UserResource("Sacane_test", pwd1.get(), "sacane.test@grostest.fr")
        userPostgresRepository.save(userEntity)
        val getUser = userPostgresRepository.findByUsername("Sacane_test")
        assertThat(getUser).isNotNull
        assertThat(getUser?.username).isEqualTo("Sacane_test")
        assertThat(Hash.contentEquals(getUser?.password!!, "01012000")).isTrue
    }


}