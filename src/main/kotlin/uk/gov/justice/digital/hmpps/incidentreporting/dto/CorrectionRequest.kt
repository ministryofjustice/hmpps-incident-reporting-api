package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

@Schema(description = "Request to make a correction to incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class CorrectionRequest(
  @Schema(description = "Why the correction is needed", required = true)
  val reason: CorrectionReason,
  @Schema(description = "The changes being requested", required = true)
  val descriptionOfChange: String,
  @Schema(description = "Member of staff requesting changed", required = true)
  val correctionRequestedBy: String,
  @Schema(description = "When the changes were requested", required = true, example = "2024-04-29T12:34:56.789012")
  val correctionRequestedAt: LocalDateTime,
)
