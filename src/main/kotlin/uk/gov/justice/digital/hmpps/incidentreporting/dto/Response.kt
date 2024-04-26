package uk.gov.justice.digital.hmpps.incidentreporting.dto

import java.time.LocalDateTime

data class Response(
  val response: String,
  val recordedBy: String,
  val recordedOn: LocalDateTime,
  val additionalInformation: String? = null,
)
