package uk.gov.justice.digital.hmpps.incidentreporting.helper

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.time.LocalDateTime

fun buildEvent(
  eventReference: String,
  /** When incident happened */
  eventDateAndTime: LocalDateTime,
  /** When report & event was created */
  reportDateAndTime: LocalDateTime,
  location: String = "MDI",
  reportingUsername: String = "USER1",
): Event {
  return Event(
    eventReference = eventReference,
    eventDateAndTime = eventDateAndTime,
    location = location,
    title = "An event occurred",
    description = "Details of the event",
    createdAt = reportDateAndTime,
    modifiedAt = reportDateAndTime,
    modifiedBy = reportingUsername,
  )
}
