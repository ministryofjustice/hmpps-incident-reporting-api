package uk.gov.justice.digital.hmpps.incidentreporting.dto

data class Question(
  val code: String,
  val question: String? = null,
  val responses: List<Response> = emptyList(),
)
