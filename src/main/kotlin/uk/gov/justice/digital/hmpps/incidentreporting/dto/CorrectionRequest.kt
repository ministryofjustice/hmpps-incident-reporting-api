package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

@Schema(description = "Request to make a correction to incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class CorrectionRequest(
  @Schema(description = "Sequence of the correction requests for this report")
  val sequence: Int,
  @Schema(description = "Why the correction is needed")
  val reason: CorrectionReason,
  @Schema(description = "The changes being requested")
  val descriptionOfChange: String,
  @Schema(description = "Member of staff requesting changed")
  val correctionRequestedBy: String,
  @Schema(description = "When the changes were requested", example = "2024-04-29T12:34:56.789012")
  val correctionRequestedAt: LocalDateTime,
)
