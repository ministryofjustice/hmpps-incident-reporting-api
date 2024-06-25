package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Update a location in an incident report")
data class UpdateLocation(
  @Schema(description = "NOMIS id of location", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val locationId: String? = null,
  @Schema(description = "Type of location", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val type: String? = null,
  @Schema(description = "Description of location", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val description: String? = null,
) {
  val isEmpty: Boolean =
    locationId == null && type == null && description == null
}
