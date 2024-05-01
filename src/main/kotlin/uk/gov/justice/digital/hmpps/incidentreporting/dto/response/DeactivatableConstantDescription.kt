package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Code and description of a constant or enumeration member which may be active or inactive")
data class DeactivatableConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", required = true, example = "DAMAGE")
  @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
  val code: String,
  @Schema(description = "Human-readable description of this value", required = true, example = "Damage")
  @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
  val description: String,
  @Schema(description = "Whether this value is currently active and usable", required = false, defaultValue = "true", example = "true")
  @JsonProperty(required = false, defaultValue = "true", access = JsonProperty.Access.READ_ONLY)
  val active: Boolean = true,
)
