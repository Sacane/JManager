package fr.sacane.jmanager.infrastructure.rest.preview

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserAccountID
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.PreviewTransactionFeature
import fr.sacane.jmanager.infrastructure.rest.id
import fr.sacane.jmanager.infrastructure.rest.preview.dto.PreviewTransactionDTO
import fr.sacane.jmanager.infrastructure.rest.preview.dto.PreviewTransactionOutDTO
import fr.sacane.jmanager.infrastructure.rest.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/transaction/preview")
@Adapter(Side.APPLICATION)
class Controller(
    private val feature: PreviewTransactionFeature
) {

    @PostMapping
    fun bookTransaction(
        @RequestHeader("Authorization") token: String,
        @RequestBody previewTransactionDTO: PreviewTransactionDTO
    ): ResponseEntity<PreviewTransactionOutDTO> {
        val tagDTO = previewTransactionDTO.tag
        return feature.bookPreviewTransaction(
            UserAccountID(
                previewTransactionDTO.userId.id(),
                previewTransactionDTO.accountId,
                AccessToken(token.asTokenUUID())
            ),
            previewTransactionDTO.label,
            previewTransactionDTO.date,
            previewTransactionDTO.value.toAmount(previewTransactionDTO.currency),
            previewTransactionDTO.isIncome,
            if(tagDTO == null) Tag("Aucune", isDefault = true) else Tag(label = tagDTO.label, id = tagDTO.tagId, isDefault = tagDTO.isDefault)
        ).map { PreviewTransactionOutDTO(it.id!!, it.label, it.amount.amount.toDouble(), it.amount.currency, it.isIncome, it.date) }
            .toResponseEntity()

    }
}