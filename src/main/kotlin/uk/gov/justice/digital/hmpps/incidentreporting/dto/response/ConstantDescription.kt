package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Code and description of a constant or enumeration member")
data class ConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", required = true, example = "VICTIM")
  @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
  val code: String,
  @Schema(description = "Human-readable description of this value", required = true, example = "Victim")
  @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
  val description: String,
)
