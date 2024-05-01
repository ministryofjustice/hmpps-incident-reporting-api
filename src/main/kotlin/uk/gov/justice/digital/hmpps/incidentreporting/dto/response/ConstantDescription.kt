package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Code and description of a constant or enumeration member")
data class ConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", required = true, example = "VICTIM")
  val code: String,
  @Schema(description = "Human-readable description of this value", required = true, example = "Victim")
  val description: String,
)
