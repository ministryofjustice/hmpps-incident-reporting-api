package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

data class CorrectionRequest(
  val reason: CorrectionReason,
  val descriptionOfChange: String,
  val correctionRequestedBy: String,
  val correctionRequestedAt: LocalDateTime,
)
