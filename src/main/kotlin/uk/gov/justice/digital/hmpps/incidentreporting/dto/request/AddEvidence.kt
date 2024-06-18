package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Add evidence to an incident report")
data class AddEvidence(
  @Schema(description = "Type of evidence", required = true)
  val type: String,
  @Schema(description = "Description of evidence", required = true)
  val description: String,
)
