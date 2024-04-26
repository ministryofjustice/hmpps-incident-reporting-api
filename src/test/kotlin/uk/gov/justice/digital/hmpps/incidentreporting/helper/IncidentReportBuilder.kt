package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

fun buildIncidentReport(
  type: Type = Type.FINDS,
  reportingUsername: String = "USER1",
  prisonId: String = "MDI",
  reportTime: LocalDateTime,
  incidentNumber: String,
  source: InformationSource = InformationSource.DPS,
): Report {
  val eventDateAndTime = reportTime.minusHours(1)
  return Report(
    incidentNumber = incidentNumber,
    prisonId = prisonId,
    incidentDateAndTime = eventDateAndTime,
    type = type,
    title = "Incident Report $incidentNumber",
    description = "A new incident created in the new service of type ${type.description}",
    reportedDate = reportTime,
    createdDate = reportTime,
    lastModifiedDate = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    lastModifiedBy = reportingUsername,
    source = source,
    event = Event(
      eventId = when (source) {
        InformationSource.DPS -> "IE-${incidentNumber.removePrefix("IR-")}"
        InformationSource.NOMIS -> incidentNumber
      },
      eventDateAndTime = eventDateAndTime,
      prisonId = prisonId,
      title = "An event occurred",
      description = "Details of the event",
      createdDate = reportTime,
      lastModifiedDate = reportTime,
      lastModifiedBy = reportingUsername,
    ),
  )
}
