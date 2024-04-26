package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime

data class History(
  val type: Type,
  val changeDate: LocalDateTime,
  val changeStaffUsername: String,
  val questions: List<HistoricalQuestion> = emptyList(),
)
