package fr.sacane.jmanager.server.spring

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.server.adapters.ServerTransactionAdapter
import fr.sacane.jmanager.server.adapters.ServerUserAdapter
import fr.sacane.jmanager.server.adapters.asResource
import fr.sacane.jmanager.server.adapters.toModel
import fr.sacane.jmanager.server.entity.CategoryResource
import fr.sacane.jmanager.server.entity.Login
import fr.sacane.jmanager.server.repositories.CategoryRepository
import fr.sacane.jmanager.server.repositories.LoginRepository
import fr.sacane.jmanager.server.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.Month
import java.util.*

@Service
class SpringLayerService {
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var loginRepository: LoginRepository
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    companion object{
        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
//        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
    }

    fun login(userPseudonym: String, password: Password): Ticket? {
        val userResponse = userRepository.findByPseudonym(userPseudonym)
        val user = userResponse ?: return null
        return if(Hash.contentEquals(user.password!!, password.value)){
            val login = loginRepository.save(Login(user, LocalDateTime.now().plusHours(1)))
            Ticket(user.toModel(), login.toModel())
        }
        else null
    }

    fun logout(userId: UserId, token: Token): Ticket? {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        loginRepository.delete(login)
        return null
    }

    fun refresh(userId: UserId, token: Token): Ticket? {
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        login.id = UUID.randomUUID()
        login.refreshToken = UUID.randomUUID()
        login.lastRefresh = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
        val response = loginRepository.save(login)
        return Ticket(user.toModel(), login.toModel())
    }

    fun getSheets(user: UserId, accountLabel: String): List<Sheet> {
        val userResource = userRepository.findById(user.get()).get()
        return userResource.accounts
            ?.find { account -> account.label == accountLabel }
            ?.sheets!!
            .map { sheetResource -> Sheet(sheetResource.idSheet!!, sheetResource.label!!, sheetResource.date!!, sheetResource.amount!!, sheetResource.isEntry!!) }
            .toList()
    }
    fun getSheetsByDateAndAccount(
        userId: UserId,
        month: Month,
        year: Int,
        labelAccount: String
    ): List<Sheet> {
        val sheets = getSheets(userId, labelAccount)
        return sheets.filter { s -> s.date.month == month && s.date.year == year }
    }
    fun getAccounts(user: UserId): List<Account> {
        return userRepository.findById(user.get())
            .get().accounts!!.distinct().map { resource -> resource.toModel() }
    }

    fun saveAccount(userId: UserId, account: Account): Account? {
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

    fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Sheet? {
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

    fun saveCategory(userId: UserId, category: Category): Category? {
        val user = userRepository.findById(userId.get()).get()
        user.categories?.add(CategoryResource(category.label)) ?: return null
        userRepository.save(user)
        return category
    }

    fun removeCategory(userId: UserId, labelCategory: String): Category? {
        val user = userRepository.findById(userId.get()).get()
        val category = user.categories?.find { it.label == labelCategory } ?: return null
        categoryRepository.deleteByLabel(labelCategory)
        return Category(category.label!!)
    }
    fun findById(userId: UserId): Ticket? {
        val user = userRepository.findById(userId.get())
        if(user.isEmpty) return null
        val token = loginRepository.findByUser(user.get()) ?: return null
        return Ticket(user.get().toModel(), token.toModel())
    }

    fun checkUser(pseudonym: String, pwd: Password): Ticket? {
        val user = userRepository.findByPseudonym(pseudonym)
        if(!MessageDigest.isEqual(pwd.get(), user?.password)){
            return null
        }
        val token = Login(user!!, LocalDateTime.now())
        val tokenBack = loginRepository.save(token)
        return Ticket(user.toModel(), Token(tokenBack.id!!, tokenBack.lastRefresh!!, tokenBack.refreshToken!!))
    }

    fun findByPseudonym(pseudonym: String): User? {
        val user = userRepository.findByPseudonym(pseudonym)
        return user?.toModel()
    }

    fun create(user: User): User?{
        return try{
            userRepository.save(user.asResource()).toModel()
        }catch(e: Exception){
            null
        }
    }

    fun save(user: User): User? {
        return try{
            val userResponse = userRepository.save(user.asResource())
            userResponse.toModel()
        }catch (e: Exception){
            null
        }
    }

    fun getUserToken(userId: UserId): Token? {
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        return loginRepository.findByUser(userResponse.get())?.toModel()
    }
}