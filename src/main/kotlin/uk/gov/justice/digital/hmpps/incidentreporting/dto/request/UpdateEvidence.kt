package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Update evidence in an incident report")
data class UpdateEvidence(
  @Schema(description = "Type of evidence", required = false, defaultValue = "null")
  val type: String? = null,
  @Schema(description = "Description of evidence", required = false, defaultValue = "null")
  val description: String? = null,
) {
  val isEmpty: Boolean =
    type == null && description == null
}
