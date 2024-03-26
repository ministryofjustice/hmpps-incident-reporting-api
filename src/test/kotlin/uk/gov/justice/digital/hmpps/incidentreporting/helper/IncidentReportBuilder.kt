package uk.gov.justice.digital.hmpps.incidentreporting.helper

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
  return IncidentReport(
    incidentNumber = incidentNumber,
    prisonId = prisonId,
    incidentDateAndTime = reportTime.minusHours(1),
    incidentType = incidentType,
    incidentDetails = "A new incident created in the new service of type ${incidentType.description}",
    reportedDate = reportTime,
    createdDate = reportTime,
    lastModifiedDate = reportTime,
    reportedBy = reportingUsername,
    assignedTo = reportingUsername,
    lastModifiedBy = reportingUsername,
    source = source,
  )
}
