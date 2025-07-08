package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Report incident type family constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class TypeFamilyConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this family", example = "DRONE_SIGHTING")
  val code: String,
  @param:Schema(description = "Human-readable description of this family", example = "Drone sighting")
  val description: String,
)
