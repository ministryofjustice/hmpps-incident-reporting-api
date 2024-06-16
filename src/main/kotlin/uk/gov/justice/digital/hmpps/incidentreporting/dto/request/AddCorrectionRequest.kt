package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

@Schema(description = "Add a correction request to an incident report")
data class AddCorrectionRequest(
  @Schema(description = "Why the correction is needed", required = true)
  val reason: CorrectionReason,
  @Schema(description = "The changes being requested", required = true)
  val descriptionOfChange: String,
  @Schema(description = "Member of staff requesting changed", required = true)
  @field:Size(min = 3, max = 120)
  val correctionRequestedBy: String,
  @Schema(description = "When the changes were requested", required = true, example = "2024-04-29T12:34:56.789012")
  val correctionRequestedAt: LocalDateTime,
)
