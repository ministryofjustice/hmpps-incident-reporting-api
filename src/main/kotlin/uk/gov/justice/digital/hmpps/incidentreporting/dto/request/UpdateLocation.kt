package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Update a location in an incident report")
data class UpdateLocation(
  @Schema(description = "NOMIS id of location", required = false, defaultValue = "null")
  val locationId: String? = null,
  @Schema(description = "Type of location", required = false, defaultValue = "null")
  val type: String? = null,
  @Schema(description = "Description of location", required = false, defaultValue = "null")
  val description: String? = null,
)
