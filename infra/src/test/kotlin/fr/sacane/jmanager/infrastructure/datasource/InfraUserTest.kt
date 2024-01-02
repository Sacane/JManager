package fr.sacane.jmanager.infrastructure.datasource

import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.infrastructure.spi.entity.AccountResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal


@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class InfraUserTest {

    @Autowired
    lateinit var userPostgresRepository: UserPostgresRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    fun basicUserTest(): UserResource {
        return UserResource("johan_test", Password("0101012000").get(),"johan.ramaroson@test.com")
    }
    @AfterEach
    fun clear(){
        userPostgresRepository.deleteByUsername("johan_test")
    }

    @Test
    @Order(1)
    fun `users should correctly be implement into database`(){
        val passwordUser = Password("01012000")
        val user = UserResource("johan_test", passwordUser.get(),"johan.ramaroson@test.com",  mutableListOf(), mutableListOf())
        userPostgresRepository.save(user)
        val byName = userPostgresRepository.findByUsername("johan_test")
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
        userPostgresRepository.save(user)

        val byName = userPostgresRepository.findByUsername(user.username)
        val account = AccountResource()
        account.amount = BigDecimal(102)
        account.label = "test account"

        byName?.accounts!!.add(account)

        userPostgresRepository.save(byName)

        assertThat(userPostgresRepository.count()).isLessThan(2)
        assertThat(byName.accounts).isNotEmpty
        assertThat(accountRepository.count()).isGreaterThan(0)
        userPostgresRepository.deleteByUsername("johan_test")

    }

}
