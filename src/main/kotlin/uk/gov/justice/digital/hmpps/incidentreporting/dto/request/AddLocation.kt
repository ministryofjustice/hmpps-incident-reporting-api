package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Add a location to an incident report")
data class AddLocation(
  @Schema(description = "NOMIS id of location", required = true, minLength = 1)
  @field:Size(min = 1)
  val locationId: String,
  @Schema(description = "Type of location", required = true, minLength = 1)
  @field:Size(min = 1)
  val type: String,
  @Schema(description = "Description of location", required = true, minLength = 1)
  @field:Size(min = 1)
  val description: String,
)
