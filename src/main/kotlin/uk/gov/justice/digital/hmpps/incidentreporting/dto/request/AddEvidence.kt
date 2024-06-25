package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Add evidence to an incident report")
data class AddEvidence(
  @Schema(description = "Type of evidence", required = true, minLength = 1)
  @field:Size(min = 1)
  val type: String,
  @Schema(description = "Description of evidence", required = true, minLength = 1)
  @field:Size(min = 1)
  val description: String,
)
