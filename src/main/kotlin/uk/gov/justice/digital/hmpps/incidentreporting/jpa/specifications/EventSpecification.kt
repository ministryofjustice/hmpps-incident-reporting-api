package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.time.LocalDate

fun filterEventsByPrisonId(prisonId: String) = Event::prisonId.buildSpecForEqualTo(prisonId)

fun filterEventsByEventDateFrom(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForGreaterThanOrEqualTo(date.atStartOfDay())

fun filterEventsByEventDateUntil(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForLessThan(date.plusDays(1).atStartOfDay())
