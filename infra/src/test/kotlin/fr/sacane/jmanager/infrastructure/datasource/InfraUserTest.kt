package fr.sacane.jmanager.infrastructure.datasource

import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.infrastructure.postgres.entity.AccountResource
import fr.sacane.jmanager.infrastructure.postgres.entity.SheetResource
import fr.sacane.jmanager.infrastructure.postgres.entity.UserResource
import fr.sacane.jmanager.infrastructure.postgres.repositories.AccountRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource



@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class InfraUserTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    fun basicUserTest(): UserResource {
        return UserResource("johan_test", Password("0101012000").get(),"johan.ramaroson@test.com")
    }
    @AfterEach
    fun clear(){
        userRepository.deleteByUsername("johan_test")
    }

    @Test
    @Order(1)
    fun `users should correctly be implement into database`(){
        val passwordUser = Password("01012000")
        val user = UserResource("johan_test", passwordUser.get(),"johan.ramaroson@test.com",  mutableListOf(), mutableListOf())
        userRepository.save(user)
        val byName = userRepository.findByUsername("johan_test")
        assertThat(byName).isNotNull
        assertThat(byName!!.username).isEqualTo(user.username)
        val password = Password.fromBytes(byName.password)
        assertThat(passwordUser.matchWith(password))

    }

    @Test
    @Order(2)
    fun `users should got accounts while add them into database`(){
        val user = basicUserTest()
        user.accounts = mutableListOf()
        userRepository.save(user)

        val byName = userRepository.findByUsername(user.username)
        val account = AccountResource()
        account.amount = 102.toDouble()
        account.label = "test account"

        byName?.accounts!!.add(account)

        userRepository.save(byName)

        assertThat(userRepository.count()).isLessThan(2)
        assertThat(byName.accounts).isNotEmpty
        assertThat(accountRepository.count()).isGreaterThan(0)
        userRepository.deleteByUsername("johan_test")

    }

}
