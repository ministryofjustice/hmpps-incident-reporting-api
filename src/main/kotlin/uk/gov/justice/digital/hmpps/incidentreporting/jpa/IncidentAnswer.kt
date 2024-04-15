package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface IncidentAnswer {
  val itemValue: String
  val recordedBy: String
  val recordedOn: LocalDateTime
  val additionalInformation: String?
}
