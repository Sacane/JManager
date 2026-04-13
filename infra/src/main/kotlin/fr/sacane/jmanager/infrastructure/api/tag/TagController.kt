package fr.sacane.jmanager.infrastructure.api.tag

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.infrastructure.api.*
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/tag")
@Adapter(Side.APPLICATION)
class TagController(
    val tagFeature: TagFeature
) {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addPersonalTag(
        @Valid @RequestBody userTagRequest: UserTagRequest
    ): ResponseEntity<TagDTO>
    = tagFeature.addTag(token = SessionToken(currentUser.token), userTagRequest.tagLabel.asPersonalTag(userTagRequest.colorDTO.asAwtColor()))
            .map { it.toDTO() }.toHttpResponse()


    @GetMapping
    fun getAllTags(
    ): ResponseEntity<List<TagDTO>>
    = tagFeature.getAllTags(SessionToken(currentUser.token)).map { it.map { tag -> tag.toDTO() } }.toHttpResponse()


    @DeleteMapping("{tagId}")
    fun deleteTag(
        @PathVariable("tagId") tagId: String,
        @RequestParam(required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<Nothing>
       = tagFeature.deleteTag(SessionToken(currentUser.token), tagId.toUUID(), force)
           .toHttpResponse()

    @GetMapping("/default")
    fun defaultTag(): ResponseEntity<TagDTO> = tagFeature.defaultTag(SessionToken(currentUser.token))
        .map { it.toDTO() }.toHttpResponse()

    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun editTag(
        @Valid @RequestBody tagDTO: TagDTO
    ): ResponseEntity<TagDTO> {
        return tagFeature.editTag(SessionToken(currentUser.token), tagDTO.toDomain())
            .map { it.toDTO() }.toHttpResponse()
    }
}