package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDateTime

data class CreateIncidentReportRequest(
  val incidentType: IncidentType,
  val incidentDateAndTime: LocalDateTime,
  val prisonId: String,
  val incidentDetails: String,
  val reportedBy: String,
  val reportedDate: LocalDateTime,
) {
  fun toNewEntity(incidentNumber: String, createdBy: String, clock: Clock): IncidentReport {
    return IncidentReport(
      incidentNumber = incidentNumber,
      incidentType = incidentType,
      incidentDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      incidentDetails = incidentDetails,
      reportedBy = reportedBy,
      reportedDate = reportedDate,
      status = IncidentStatus.DRAFT,
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = createdBy,
      source = InformationSource.DPS,
      assignedTo = reportedBy
    )
  }
}
