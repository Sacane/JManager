package fr.sacane.jmanager.application.api.tag

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.port.input.tag.AddTagUseCase
import fr.sacane.jmanager.domain.port.input.tag.DefaultTagUseCase
import fr.sacane.jmanager.domain.port.input.tag.DeleteTagUseCase
import fr.sacane.jmanager.domain.port.input.tag.EditTagUseCase
import fr.sacane.jmanager.domain.port.input.tag.GetAllTagsUseCase
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.application.api.*
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/tag")
@Adapter(Side.APPLICATION)
class TagController(
    val addTagUseCase: AddTagUseCase,
    val getAllTagsUseCase: GetAllTagsUseCase,
    val deleteTagUseCase: DeleteTagUseCase,
    val defaultTagUseCase: DefaultTagUseCase,
    val editTagUseCase: EditTagUseCase,
) {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addPersonalTag(
        @Valid @RequestBody userTagRequest: UserTagRequest
    ): ResponseEntity<TagDTO>
    = addTagUseCase.addTag(token = SessionToken(currentUser.token), userTagRequest.tagLabel.asPersonalTag(userTagRequest.colorDTO.asAwtColor()))
            .map { it.toDTO() }.toHttpResponse()


    @GetMapping
    fun getAllTags(
    ): ResponseEntity<List<TagDTO>>
    = getAllTagsUseCase.getAllTags(SessionToken(currentUser.token)).map { it.map { tag -> tag.toDTO() } }.toHttpResponse()


    @DeleteMapping("{tagId}")
    fun deleteTag(
        @PathVariable("tagId") tagId: String,
        @RequestParam(required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<Nothing>
       = deleteTagUseCase.deleteTag(SessionToken(currentUser.token), tagId.toUUID(), force)
           .toHttpResponse()

    @GetMapping("/default")
    fun defaultTag(): ResponseEntity<TagDTO> = defaultTagUseCase.defaultTag(SessionToken(currentUser.token))
        .map { it.toDTO() }.toHttpResponse()

    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun editTag(
        @Valid @RequestBody tagDTO: TagDTO
    ): ResponseEntity<TagDTO> {
        return editTagUseCase.editTag(SessionToken(currentUser.token), tagDTO.toDomain())
            .map { it.toDTO() }.toHttpResponse()
    }
}