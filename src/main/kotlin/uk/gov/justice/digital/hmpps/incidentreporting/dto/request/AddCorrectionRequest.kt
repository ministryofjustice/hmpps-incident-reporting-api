package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason

@Schema(description = "Add a correction request to an incident report")
data class AddCorrectionRequest(
  @Schema(description = "Why the correction is needed", required = true)
  val reason: CorrectionReason,
  @Schema(description = "The changes being requested", required = true)
  @field:Size(min = 1)
  val descriptionOfChange: String,
)
