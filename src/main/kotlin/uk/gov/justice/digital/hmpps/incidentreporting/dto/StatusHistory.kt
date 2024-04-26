package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import java.time.LocalDateTime

data class StatusHistory(
  val status: Status,
  val setOn: LocalDateTime,
  val setBy: String,
)
