package fr.sacane.jmanager.infrastructure.api.tag

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.infrastructure.api.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/tag")
@Adapter(Side.APPLICATION)
class TagController(
    val tagFeature: TagFeature
) {

    @PostMapping
    fun addPersonalTag(
        @RequestBody userTagRequest: UserTagRequest
    ): ResponseEntity<TagDTO>
    = tagFeature.addTag(token = currentUser.token, userTagRequest.tagLabel.asPersonalTag(userTagRequest.colorDTO.asAwtColor()))
            .map { it.toDTO() }.toHttpResponse()


    @GetMapping
    fun getAllTags(
    ): ResponseEntity<List<TagDTO>>
    = tagFeature.getAllTags(currentUser.token).map { it.map { tag -> tag.toDTO() } }.toHttpResponse()


    @DeleteMapping("{tagId}")
    fun deleteTag(
        @PathVariable("tagId") tagId: String,
        @RequestParam(required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<Nothing>
       = tagFeature.deleteTag(currentUser.token, tagId.toUUID(), force)
           .toHttpResponse()

    @GetMapping("/default")
    fun defaultTag(): ResponseEntity<TagDTO> = tagFeature.defaultTag(currentUser.token)
        .map { it.toDTO() }.toHttpResponse()

    @PatchMapping
    fun editTag(
        @RequestBody tagDTO: TagDTO
    ): ResponseEntity<TagDTO> {
        return tagFeature.editTag(currentUser.token, tagDTO.toDomain())
            .map { it.toDTO() }.toHttpResponse()
    }
}