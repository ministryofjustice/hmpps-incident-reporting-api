package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface GenericResponse {
  // TODO: should we add a `val code: String` like in Question?
  val response: String
  val additionalInformation: String?
  val recordedBy: String
  val recordedOn: LocalDateTime
}
