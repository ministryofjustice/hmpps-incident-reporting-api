package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

fun buildIncidentReport(
  incidentNumber: String,
  reportTime: LocalDateTime,
  prisonId: String = "MDI",
  source: InformationSource = InformationSource.DPS,
  status: Status = Status.DRAFT,
  type: Type = Type.FINDS,
  reportingUsername: String = "USER1",
): Report {
  val eventDateAndTime = reportTime.minusHours(1)
  val report = Report(
    incidentNumber = incidentNumber,
    incidentDateAndTime = eventDateAndTime,
    prisonId = prisonId,
    source = source,
    status = status,
    type = type,
    title = "Incident Report $incidentNumber",
    description = "A new incident created in the new service of type ${type.description}",
    reportedDate = reportTime,
    createdDate = reportTime,
    lastModifiedDate = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    lastModifiedBy = reportingUsername,
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
  report.addStatusHistory(report.status, reportTime, reportingUsername)
  return report
}
