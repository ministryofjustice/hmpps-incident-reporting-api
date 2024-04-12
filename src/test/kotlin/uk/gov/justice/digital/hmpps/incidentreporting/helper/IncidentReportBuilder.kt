package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.LocalDateTime

fun buildIncidentReport(
  incidentType: IncidentType = IncidentType.FINDS,
  reportingUsername: String = "USER1",
  prisonId: String = "MDI",
  reportTime: LocalDateTime,
  incidentNumber: String,
  source: InformationSource = InformationSource.DPS,
): IncidentReport {
  val eventDateAndTime = reportTime.minusHours(1)
  return IncidentReport(
    incidentNumber = incidentNumber,
    prisonId = prisonId,
    incidentDateAndTime = eventDateAndTime,
    incidentType = incidentType,
    summary = "Incident Report $incidentNumber",
    incidentDetails = "A new incident created in the new service of type ${incidentType.description}",
    reportedDate = reportTime,
    createdDate = reportTime,
    lastModifiedDate = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    lastModifiedBy = reportingUsername,
    source = source,
    event = IncidentEvent(
      eventId = when (source) {
        InformationSource.DPS -> "IE-${incidentNumber.removePrefix("IR-")}"
        InformationSource.NOMIS -> incidentNumber
      },
      eventDateAndTime = eventDateAndTime,
      prisonId = prisonId,
      eventDetails = "An event occurred",
      createdDate = reportTime,
      lastModifiedDate = reportTime,
      lastModifiedBy = reportingUsername,
    ),
  )
}
