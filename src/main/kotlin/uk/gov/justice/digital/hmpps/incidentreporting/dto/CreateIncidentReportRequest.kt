package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent
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
  val summary: String? = null,
  val createNewEvent: Boolean = false,
  val linkedEventId: String? = null,
  val incidentDetails: String,
  val reportedBy: String,
  val reportedDate: LocalDateTime,
) {
  fun toNewEntity(incidentNumber: String, event: IncidentEvent? = null, createdBy: String, clock: Clock): IncidentReport {
    return IncidentReport(
      incidentNumber = incidentNumber,
      incidentType = incidentType,
      summary = summary ?: "Incident Report $incidentNumber",
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
      assignedTo = reportedBy,
      event = event,
    )
  }

  fun toNewEvent(generateEventId: String, createdBy: String, clock: Clock): IncidentEvent {
    return IncidentEvent(
      eventId = generateEventId,
      eventDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      eventDetails = incidentDetails,
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = createdBy,
    )
  }
}
