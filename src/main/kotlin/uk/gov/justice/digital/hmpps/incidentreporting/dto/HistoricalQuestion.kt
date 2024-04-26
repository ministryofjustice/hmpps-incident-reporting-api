package uk.gov.justice.digital.hmpps.incidentreporting.dto

data class HistoricalQuestion(
  val code: String,
  val question: String? = null,
  val responses: List<HistoricalResponse> = emptyList(),
)
