package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface GenericQuestion {
  val code: String

  // TODO: should we force `question` to be non-null?
  val question: String?

  fun getResponses(): List<GenericResponse>

  fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): GenericQuestion
}
