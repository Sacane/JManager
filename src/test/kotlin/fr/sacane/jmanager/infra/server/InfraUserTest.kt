package fr.sacane.jmanager.infra.server

import fr.sacane.jmanager.infra.server.entity.AccountResource
import fr.sacane.jmanager.infra.server.entity.SheetResource
import fr.sacane.jmanager.infra.server.entity.UserResource
import fr.sacane.jmanager.infra.server.repositories.AccountRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class InfraUserTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    fun basicUserTest(): UserResource {
        return UserResource(null, "johan_test", "johan.ramaroson@test.com", "01012000", "Sacane", null)
    }

    fun basicSheetTest(): SheetResource{
        return SheetResource()
    }

    @Test
    @Order(1)
    fun `users should correctly be implement into database`(){
        userRepository.deleteAll()
        val user = UserResource(null, "johan_test", "johan.ramaroson@test.com", "01012000", "Sacane", mutableListOf())
        userRepository.save(user)
        val byName = userRepository.findByPseudonym("johan_test")
        assertThat(byName.pseudonym).isEqualTo(user.pseudonym)
        userRepository.deleteById(byName.id_user!!)

    }

    fun `sheets on account should be created correctly`(){

    }

    @Test
    @Order(2)
    fun `users should got accounts while add them into database`(){
        userRepository.deleteByPseudonym("johan_test")
        val user = basicUserTest()
        user.accounts = mutableListOf()
        userRepository.save(user)

        val byName = userRepository.findByPseudonym(user.pseudonym!!)
        val account = AccountResource()
        account.amount = 102.toDouble()
        account.label = "test account"

        byName.accounts!!.add(account)

        userRepository.save(byName)

        assertThat(userRepository.count()).isLessThan(2)
        assertThat(byName.accounts).isNotEmpty
        assertThat(accountRepository.count()).isGreaterThan(0)
        userRepository.deleteByPseudonym("johan_test")

    }

}
