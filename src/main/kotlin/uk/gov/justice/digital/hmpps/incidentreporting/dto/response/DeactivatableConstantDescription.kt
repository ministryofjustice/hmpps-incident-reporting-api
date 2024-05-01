package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Code and description of a constant or enumeration member which may be active or inactive")
data class DeactivatableConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", required = true, example = "DAMAGE")
  val code: String,
  @Schema(description = "Human-readable description of this value", required = true, example = "Damage")
  val description: String,
  @Schema(description = "Whether this value is currently active and usable", required = false, defaultValue = "true", example = "true")
  val active: Boolean = true,
)
