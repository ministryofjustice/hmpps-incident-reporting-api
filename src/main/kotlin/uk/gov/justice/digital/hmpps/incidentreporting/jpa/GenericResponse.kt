package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface GenericResponse {
  val response: String
  val recordedBy: String
  val recordedOn: LocalDateTime
  val additionalInformation: String?
}
