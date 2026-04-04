package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.ForeverEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.JpaRegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RecurrenceRuleEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RecurrenceType
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionTrackerEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class IdTest {

    companion object {
        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("test")
            .withUsername("sa")
            .withPassword("sa")

        init {
            postgresContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
        }
    }

    @Autowired
    lateinit var userRepo: UserPostgresRepository

    @Autowired
    lateinit var bookletRepo: BookletJpaRepository

    @Autowired
    lateinit var transactionRepo: TransactionJpaRepository

    @Autowired
    lateinit var defaultTagRepo: DefaultTagPostgresRepository

    @Autowired
    lateinit var personalTagRepo: TagPersonalPostgresRepository

    @Autowired
    lateinit var regularTransactionRepo: RegularTransactionResourceJpaRepository

    @Autowired
    lateinit var trackerRepo: JpaRegularTransactionTrackerRepository

    @AfterEach
    fun cleanup() {
        // best-effort cleanup
        trackerRepo.deleteAll()
        regularTransactionRepo.deleteAll()
        transactionRepo.deleteAll()
        bookletRepo.deleteAll()
        defaultTagRepo.deleteAll()
        personalTagRepo.deleteAll()
        userRepo.deleteAll()
    }

    @Test
    fun `when saving user with null id postgres generates uuid`() {
        val user = UserResource("user_test", "pwd", "email@test.com")
        user.idUser = null
        val saved = userRepo.save(user)
        assertThat(saved.idUser).isNotNull()
    }

    @Test
    fun `when saving booklet with null id postgres generates uuid`() {
        val user = UserResource("owner_test", "pwd", "owner@test.com")
        val owner = userRepo.save(user)

        val booklet = BookletResource(label = "my acc")
        booklet.idBooklet = null
        booklet.owner = owner
        val saved = bookletRepo.save(booklet)
        assertThat(saved.idBooklet).isNotNull()
    }

    @Test
    fun `when saving transaction with null id postgres generates uuid`() {
        val owner = userRepo.save(UserResource("owner_tx", "pwd", "owner_tx@test.com"))
        val booklet = bookletRepo.save(BookletResource(label = "acc tx", owner = owner))

        val tx = TransactionResource(label = "tx", account = booklet)
        tx.idSheet = null
        val saved = transactionRepo.save(tx)
        assertThat(saved.idSheet).isNotNull()
    }

    @Test
    fun `when saving tags with null id postgres generates uuid`() {
        val defaultTag = DefaultTagResource(name = "default")
        val personalTag = TagPersonalResource(name = "pers")

        val savedDefault = defaultTagRepo.save(defaultTag)
        val savedPersonal = personalTagRepo.save(personalTag)

        assertThat(savedDefault.idTag).isNotNull()
        assertThat(savedPersonal.idTag).isNotNull()
    }

    @Test
    fun `when saving regular transaction with null id postgres generates uuid`() {
        val owner = userRepo.save(UserResource("owner_rt", "pwd", "owner_rt@test.com"))
        val booklet = bookletRepo.save(BookletResource(label = "acc rt", owner = owner))

        val freq = ForeverEntity()
        val recurrence = RecurrenceRuleEntity(type = RecurrenceType.MONTHLY, dayOfMonth = 1)

        val regular = RegularTransactionEntity(
            transactionId = null,
            startDate = LocalDate.now(),
            label = "reg",
            amount = 1.0,
            isIncome = false,
            frequencyProperty = freq,
            recurrenceRule = recurrence,
            tag = null,
            personalTag = null,
            accounts = mutableSetOf(booklet),
            owner = owner
        )

        val saved = regularTransactionRepo.save(regular)
        assertThat(saved.transactionId).isNotNull()
    }

    @Test
    fun `when saving tracker with null id postgres generates id`() {
        val tracker = RegularTransactionTrackerEntity(
            regularTransactionId = "rt-id",
            bookletId = UUID.randomUUID(),
            lastGeneratedDate = LocalDate.now(),
            numberOfGeneratedTransaction = 0,
            id = null
        )

        val saved = trackerRepo.save(tracker)
        assertThat(saved.id).isNotNull()
    }
}
