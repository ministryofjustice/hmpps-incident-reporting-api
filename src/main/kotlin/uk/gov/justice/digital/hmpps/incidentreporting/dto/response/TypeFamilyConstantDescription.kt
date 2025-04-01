package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Report type family constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class TypeFamilyConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", example = "DRONE_SIGHTING")
  val code: String,
  @Schema(description = "Human-readable description of this value", example = "Drone sighting")
  val description: String,
)
