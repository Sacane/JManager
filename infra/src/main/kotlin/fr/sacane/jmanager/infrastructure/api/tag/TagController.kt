package fr.sacane.jmanager.infrastructure.api.tag

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.infrastructure.api.asAwtColor
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
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
        @RequestBody userTagRequest: UserTagRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<TagDTO>
    = tagFeature.addTag(token = token.asTokenUUID(), userTagRequest.tagLabel.asPersonalTag(userTagRequest.colorDTO.asAwtColor()))
            .map { it.toDTO() }.toHttpResponse()


    @GetMapping
    fun getAllTags(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<List<TagDTO>>
    = tagFeature.getAllTags(token.asTokenUUID()).map { it.map { tag -> tag.toDTO() } }.toHttpResponse()


    @DeleteMapping("{tagId}")
    fun deleteTag(
        @RequestHeader("Authorization") token: String,
        @PathVariable("tagId") tagId: Long
    ): ResponseEntity<Nothing>
       = tagFeature.deleteTag(token.asTokenUUID(), tagId)
           .toHttpResponse()

    @GetMapping("/default")
    fun defaultTag(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<TagDTO> = tagFeature.defaultTag(token.asTokenUUID())
        .map { it.toDTO() }.toHttpResponse()

}