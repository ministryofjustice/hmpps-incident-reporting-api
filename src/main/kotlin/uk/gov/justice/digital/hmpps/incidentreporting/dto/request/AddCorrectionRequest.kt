package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason

@Schema(description = "Add a correction request to an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddCorrectionRequest(
  @Schema(description = "Why the correction is needed", requiredMode = Schema.RequiredMode.REQUIRED)
  val reason: CorrectionReason,
  @Schema(description = "The changes being requested", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Size(min = 1)
  val descriptionOfChange: String,
)
