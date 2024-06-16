package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

@Schema(description = "Update a correction request in an incident report")
data class UpdateCorrectionRequest(
  @Schema(description = "Why the correction is needed", required = false, defaultValue = "null")
  val reason: CorrectionReason? = null,
  @Schema(description = "The changes being requested", required = false, defaultValue = "null")
  val descriptionOfChange: String? = null,
  @Schema(description = "Member of staff requesting changed", required = false, defaultValue = "null")
  val correctionRequestedBy: String? = null,
  @Schema(description = "When the changes were requested", required = false, defaultValue = "null", example = "2024-04-29T12:34:56.789012")
  val correctionRequestedAt: LocalDateTime? = null,
)
