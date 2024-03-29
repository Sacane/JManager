package fr.sacane.jmanager.infrastructure.postgres.adapters
//
//import fr.sacane.jmanager.domain.Hash
//import fr.sacane.jmanager.domain.hexadoc.Adapter
//import fr.sacane.jmanager.domain.hexadoc.Side
//import fr.sacane.jmanager.domain.models.*
//import fr.sacane.jmanager.infrastructure.Environment
//import fr.sacane.jmanager.infrastructure.postgres.entity.Login
//import fr.sacane.jmanager.infrastructure.postgres.repositories.LoginRepository
//import fr.sacane.jmanager.infrastructure.postgres.repositories.UserPostgresRepository
//import fr.sacane.jmanager.infrastructure.rest.InvalidRequestException
//import org.springframework.stereotype.Service
//import java.time.LocalDateTime
//import java.util.*
//import java.util.logging.Logger
//
//@Service
//@Adapter(Side.DATASOURCE)
//class LoginTransactionAdapter(
//    private val userPostgresRepository: UserPostgresRepository,
//    private val loginRepository: LoginRepository
//) : SessionRepository {
//    companion object{
//        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
////        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
//        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
//    }
//    override fun login(userPseudonym: String, password: Password): UserToken? {
//        LOGGER.info("Trying to login user $userPseudonym")
//        val userResponse = userPostgresRepository.findByUsername(userPseudonym) ?: return null
//        LOGGER.info("Find user ${userResponse.username}")
//        val pwd = password.value ?: return null
//        return if(Hash.contentEquals(userResponse.password, pwd)){
//            val login = loginRepository.save(
//                Login(
//                    user=userResponse,
//                    tokenLifeTime = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
//                )
//            )
//            LOGGER.info("User ${userResponse.username} logged in")
//            UserToken(userResponse.toModel(), login.toModel())
//        }
//        else null
//    }
//
//    override fun logout(userId: UserId, token: AccessToken): AccessToken? {
//        val id = userId.id ?: return null
//        val userResponse = userPostgresRepository.findById(id)
//        if(userResponse.isEmpty) return null
//        val user = userResponse.get()
//        val login = loginRepository.findByUser(user) ?: return null
//        loginRepository.delete(login)
//        return login.toModel()
//    }
//
//    override fun refresh(userId: UserId, token: AccessToken): UserToken? {
//        val id = userId.id ?: return null
//        val userResponse = userPostgresRepository.findById(id)
//        if (userResponse.isEmpty) return null
//        val user = userResponse.get()
//        val login = loginRepository.findByUser(user) ?: return null
//        login.id = UUID.randomUUID()
//        login.refreshToken = UUID.randomUUID()
//        login.tokenLifeTime = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
//        loginRepository.save(login)
//        return UserToken(user.toModel(), login.toModel())
//    }
//
//    override fun tokenBy(userId: UserId): UserToken? {
//        val id = userId.id ?: return null
//        val user = userPostgresRepository.findById(id)
//        if(user.isEmpty) return null
//        val token = loginRepository.findByUser(user.get()) ?: return null
//        return UserToken(user.get().toModel(), token.toModel())
//    }
//
//    override fun generateToken(user: User): AccessToken? {
//        val id = user.id.id ?: return null
//        val userResponse = userPostgresRepository.findById(id)
//        if(userResponse.isEmpty) return null
//        return loginRepository
//            .save(
//                Login(
//                user = userResponse.get(),
//                tokenLifeTime = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS))
//            ).toModel()
//    }
//
//    override fun deleteToken(userId: UserId) {
//        val id = userId.id ?: return
//        val user = userPostgresRepository.findById(id)
//            .orElse(null) ?: return
//        val token = loginRepository.findByUser(user) ?: return
//        loginRepository.deleteById(token.id ?: throw InvalidRequestException("Impossible de supprimer le token car son ID est null"))
//    }
//
//    override fun deleteTokens(tokens: List<AccessToken>) {
//        TODO()
//    }
//
//    override fun refreshTokenLifetime(userID: UserId, refreshLifeTime: Boolean): AccessToken? {
//        val id = userID.id ?: return null
//        val user = userPostgresRepository.findById(id)
//            .orElse(null) ?: return null
//        val token = loginRepository.findByUser(user) ?: return null
//        return loginRepository.save(token.also {
//            it.tokenLifeTime = LocalDateTime.now().plusHours(Environment.DEFAULT_TOKEN_LIFETIME_IN_HOURS)
//            if(refreshLifeTime) {
//                it.refreshTokenLifetime = LocalDateTime.now().plusDays(Environment.DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS)
//            }
//        }).toModel()
//    }
//
//    override fun save(token: AccessToken): AccessToken? {
//        TODO()
//    }
//
//    override fun tokens(): List<AccessToken> {
//        return loginRepository.findAll().map {
//            it.toModel()
//        }
//    }
//
//}