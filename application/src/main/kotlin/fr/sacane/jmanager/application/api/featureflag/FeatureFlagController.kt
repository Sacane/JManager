package fr.sacane.jmanager.application.api.featureflag

import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.bus.CommandBus
import fr.sacane.jmanager.application.bus.QueryBus
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.featureflag.GetAllFeatureFlagsQuery
import fr.sacane.jmanager.domain.port.input.featureflag.ToggleFeatureFlagCommand
import fr.sacane.jmanager.domain.utils.ResultState
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class FeatureFlagDTO(val key: String, val enabled: Boolean)
data class ToggleFeatureFlagRequest(val enabled: Boolean)

@RestController
@Adapter(Side.APPLICATION)
class FeatureFlagReadController(
    private val queryBus: QueryBus,
) {
    @GetMapping("/api/feature-flags")
    fun getAll(): ResponseEntity<List<FeatureFlagDTO>> =
        queryBus.dispatch(GetAllFeatureFlagsQuery())
            .map { flags -> flags.map { FeatureFlagDTO(it.key.name, it.enabled) } }
            .toHttpResponse()
}

@RestController
@RequestMapping("/api/admin/feature-flags")
@Adapter(Side.APPLICATION)
class FeatureFlagAdminController(
    private val commandBus: CommandBus,
) {
    private val log = LoggerFactory.getLogger(FeatureFlagAdminController::class.java)

    @PatchMapping("/{key}")
    fun toggle(
        @PathVariable key: String,
        @Valid @RequestBody request: ToggleFeatureFlagRequest,
    ): ResponseEntity<FeatureFlagDTO> {
        val featureKey = parseFeatureKey(key)
        return commandBus.dispatch(ToggleFeatureFlagCommand(featureKey, request.enabled))
            .map { FeatureFlagDTO(it.key.name, it.enabled) }
            .toHttpResponse()
    }

    private fun parseFeatureKey(raw: String): FeatureKey =
        FeatureKey.entries.firstOrNull { it.name == raw }
            ?: run {
                log.warn("Unknown feature flag key requested: '{}'", raw)
                throw InvalidRequestException(
                    ResultState.INVALID.code,
                    "Unknown feature flag key: '$raw'",
                    "feature_flag.unknown_key",
                )
            }
}
