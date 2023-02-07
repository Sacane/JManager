package fr.sacane.jmanager.server

import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.server.entity.AccountResource
import fr.sacane.jmanager.server.entity.SheetResource
import fr.sacane.jmanager.server.entity.UserResource
import fr.sacane.jmanager.server.repositories.AccountRepository
import fr.sacane.jmanager.server.repositories.UserRepository
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
        return UserResource(null, "johan_test", "johan.ramaroson@test.com", Password("0101012000").get(), "Sacane", null,null)
    }

    fun basicSheetTest(): SheetResource {
        return SheetResource()
    }

    @AfterEach
    fun clear(){
        userRepository.deleteByPseudonym("johan_test")
    }

    @Test
    @Order(1)
    fun `users should correctly be implement into database`(){
        val user = UserResource("johan_test", "johan.ramaroson@test.com", Password("01012000").get(), "Sacane", mutableListOf(), mutableListOf())
        userRepository.save(user)
        val byName = userRepository.findByPseudonym("johan_test")
        assertThat(byName?.pseudonym).isEqualTo(user.pseudonym)


    }

    fun `sheets on account should be created correctly`(){

    }

    @Test
    @Order(2)
    fun `users should got accounts while add them into database`(){
        val user = basicUserTest()
        user.accounts = mutableListOf()
        userRepository.save(user)

        val byName = userRepository.findByPseudonym(user.pseudonym!!)
        val account = AccountResource()
        account.amount = 102.toDouble()
        account.label = "test account"

        byName?.accounts!!.add(account)

        userRepository.save(byName)

        assertThat(userRepository.count()).isLessThan(2)
        assertThat(byName.accounts).isNotEmpty
        assertThat(accountRepository.count()).isGreaterThan(0)
        userRepository.deleteByPseudonym("johan_test")

    }

}
