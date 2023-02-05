package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.common.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.infra.server.entity.CategoryResource
import fr.sacane.jmanager.infra.server.repositories.CategoryRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository


    override fun getSheets(user: UserId, accountLabel: String): List<Sheet> {
        val userResource = userRepository.findById(user.get()).get()
        return userResource.accounts
            ?.find { account -> account.label == accountLabel }
            ?.sheets!!
            .map { sheetResource -> Sheet(sheetResource.idSheet!!, sheetResource.label!!, sheetResource.date!!, sheetResource.amount!!, sheetResource.isEntry!!) }
            .toList()
    }
    override fun getSheetsByDateAndAccount(
        userId: UserId,
        month: Month,
        year: Int,
        labelAccount: String
    ): List<Sheet> {
        val sheets = getSheets(userId, labelAccount)
        return sheets.filter { s -> s.date.month == month && s.date.year == year }
    }
    override fun getAccounts(user: UserId): List<Account> {
        logger.debug("Trying to reach accounts of user ${user.get()}")
        return userRepository.findById(user.get())
            .get().accounts!!.distinct().map { resource -> resource.toModel() }
    }

    override fun saveAccount(userId: UserId, account: Account): Account? {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return null
        val user = userResponse.get()
        user.accounts?.add(account.asResource())
        return try{
            userRepository.save(user)
            account
        }catch (e: Exception){
            null
        }
    }

    override fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Sheet? {
        val user = userRepository.findById(userId.get()).get()
        val account = user.accounts?.find { it.label == accountLabel } ?: return null
        return try{
            account.sheets?.add(sheet.asResource())
            account.amount = if(sheet.isEntry) account.amount?.plus(sheet.value) else account.amount?.minus(sheet.value)
            userRepository.save(user)
            sheet
        }catch(e: Exception){
            null
        }
    }

    override fun saveCategory(userId: UserId, category: Category): Category? {
        val user = userRepository.findById(userId.get()).get()
        user.categories?.add(CategoryResource(category.label)) ?: return null
        userRepository.save(user)
        return category
    }

    override fun removeCategory(userId: UserId, labelCategory: String): Category? {
        val user = userRepository.findById(userId.get()).get()
        val category = user.categories?.find { it.label == labelCategory } ?: return null
        categoryRepository.deleteByLabel(labelCategory)
        return Category(category.label!!)
    }
}
