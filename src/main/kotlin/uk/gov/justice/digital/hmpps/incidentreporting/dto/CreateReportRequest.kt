package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.Clock
import java.time.LocalDateTime

data class CreateReportRequest(
  val type: Type,
  val incidentDateAndTime: LocalDateTime,
  val prisonId: String,
  val title: String,
  val description: String,
  val createNewEvent: Boolean = false,
  val linkedEventId: String? = null,
  val reportedBy: String,
  val reportedDate: LocalDateTime,
) {
  fun toNewEntity(incidentNumber: String, event: Event, createdBy: String, clock: Clock): Report {
    return Report(
      incidentNumber = incidentNumber,
      type = type,
      title = title,
      incidentDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      description = description,
      reportedBy = reportedBy,
      reportedDate = reportedDate,
      status = Status.DRAFT,
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = createdBy,
      source = InformationSource.DPS,
      assignedTo = reportedBy,
      event = event,
    )
  }

  fun toNewEvent(generateEventId: String, createdBy: String, clock: Clock): Event {
    return Event(
      eventId = generateEventId,
      eventDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      title = title,
      description = description,
      createdDate = LocalDateTime.now(clock),
      lastModifiedDate = LocalDateTime.now(clock),
      lastModifiedBy = createdBy,
    )
  }
}
