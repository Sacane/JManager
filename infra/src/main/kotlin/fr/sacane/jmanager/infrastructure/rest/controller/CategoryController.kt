package fr.sacane.jmanager.infrastructure.rest.controller


import fr.sacane.jmanager.infrastructure.JmanagerApplication
import fr.sacane.jmanager.infrastructure.rest.adapters.TransactionValidator
import fr.sacane.jmanager.infrastructure.rest.adapters.UserControlAdapter
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.rest.TokenDTO
import fr.sacane.jmanager.infrastructure.rest.UserCategoryDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CategoryController {
    companion object{
        private val LOGGER: Logger = LoggerFactory.getLogger(JmanagerApplication::class.java.name)
    }

    @Autowired
    private lateinit var apiAdapter: TransactionValidator

    @Autowired
    private lateinit var userAdapter: UserControlAdapter


    @PostMapping(path = ["user/category"])
    suspend fun saveUserCategory(@RequestBody userCategoryDTO: UserCategoryDTO, @RequestHeader token: TokenDTO): ResponseEntity<String>{
        LOGGER.info("Add a new Category")
        return apiAdapter.saveCategory(userCategoryDTO, token)
    }

    @GetMapping(path = ["user/categories/{userId}"])
    suspend fun retrieveAllUserCategories(@PathVariable userId: String, @RequestHeader("Authorization") token: TokenDTO): ResponseEntity<List<String>>{
        return apiAdapter.retrieveAllCategories(UserId(userId.toLong()), token)
    }
    @DeleteMapping(path=["category/delete"])
    suspend fun deleteCategory(@RequestBody userCategoryDTO: UserCategoryDTO, @RequestHeader token: TokenDTO): ResponseEntity<String>{
        return apiAdapter.removeCategory(userCategoryDTO, token)
    }
}

