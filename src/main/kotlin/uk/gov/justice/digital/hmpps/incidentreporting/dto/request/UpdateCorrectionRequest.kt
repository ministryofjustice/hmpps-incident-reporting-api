package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason

@Schema(description = "Update a correction request in an incident report")
data class UpdateCorrectionRequest(
  @Schema(description = "Why the correction is needed", required = false, defaultValue = "null")
  val reason: CorrectionReason? = null,
  @Schema(description = "The changes being requested", required = false, defaultValue = "null", minLength = 1)
  @field:Size(min = 1)
  val descriptionOfChange: String? = null,
) {
  val isEmpty: Boolean =
    reason == null && descriptionOfChange == null
}
