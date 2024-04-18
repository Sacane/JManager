package fr.sacane.jmanager.infrastructure.rest.tag

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.models.asTag
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.infrastructure.rest.asAwtColor
import fr.sacane.jmanager.infrastructure.rest.id
import fr.sacane.jmanager.infrastructure.rest.toDTO
import fr.sacane.jmanager.infrastructure.rest.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/tag")
@Adapter(Side.API)
class TagController(
    val tagFeature: TagFeature
) {

    @PostMapping
    fun addPersonalTag(
        @RequestBody userTagDTO: UserTagDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<TagDTO>
    = tagFeature.addTag(userId = userTagDTO.userId.id(), token = token.asTokenUUID(), userTagDTO.tagLabel.asPersonalTag(userTagDTO.colorDTO.asAwtColor()))
            .map { TagDTO(it.id!!, label = it.label, isDefault =  it.isDefault) }.toResponseEntity()


    @GetMapping("/user/{userId}")
    fun getAllTags(
        @RequestHeader("Authorization") token: String,
        @PathVariable("userId") userId: Long
    ): ResponseEntity<List<TagDTO>>
    = tagFeature.getAllTags(userId.id(), token.asTokenUUID()).map { it.map { tag -> tag.toDTO() } }.toResponseEntity()

}