package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Update evidence in an incident report")
data class UpdateEvidence(
  @Schema(description = "Type of evidence", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val type: String? = null,
  @Schema(description = "Description of evidence", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val description: String? = null,
) {
  val isEmpty: Boolean =
    type == null && description == null
}
