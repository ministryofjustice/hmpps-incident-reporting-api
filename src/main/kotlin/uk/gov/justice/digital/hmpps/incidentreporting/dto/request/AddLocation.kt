package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Add a location to an incident report")
data class AddLocation(
  @Schema(description = "NOMIS id of location", required = true)
  val locationId: String,
  @Schema(description = "Type of location", required = true)
  val type: String,
  @Schema(description = "Description of location", required = true)
  val description: String,
)
