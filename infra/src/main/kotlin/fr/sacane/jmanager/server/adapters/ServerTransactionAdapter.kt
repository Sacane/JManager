package fr.sacane.jmanager.server.adapters

import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.server.entity.CategoryResource
import fr.sacane.jmanager.server.repositories.CategoryRepository
import fr.sacane.jmanager.server.repositories.UserRepository
import fr.sacane.jmanager.server.spring.SpringLayerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception
import java.time.Month

@Service
@DatasourceAdapter
class ServerTransactionAdapter : TransactionRegister{

    companion object{
        private val logger = LoggerFactory.getLogger("infra.server.adapters.ServerAdapter")
    }

    @Autowired
    private lateinit var service: SpringLayerService


    override fun getSheets(user: UserId, accountLabel: String): List<Sheet> {
        return service.getSheets(user, accountLabel)
    }
    override fun getSheetsByDateAndAccount(
        userId: UserId,
        month: Month,
        year: Int,
        labelAccount: String
    ): List<Sheet> {
        return service.getSheetsByDateAndAccount(userId, month, year, labelAccount)
    }
    override fun getAccounts(user: UserId): List<Account> {
        logger.debug("Trying to reach accounts of user ${user.get()}")
        return service.getAccounts(user)
    }

    override fun saveAccount(userId: UserId, account: Account): Account? {
        return service.saveAccount(userId, account)
    }

    override fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Sheet? {
        return service.saveSheet(userId, accountLabel, sheet)
    }

    override fun saveCategory(userId: UserId, category: Category): Category? {
        return service.saveCategory(userId, category)
    }

    override fun removeCategory(userId: UserId, labelCategory: String): Category? {
        return service.removeCategory(userId, labelCategory)
    }
}
