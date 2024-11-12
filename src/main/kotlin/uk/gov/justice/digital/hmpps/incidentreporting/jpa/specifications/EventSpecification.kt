package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.time.LocalDate

fun filterEventsByPrisonIds(prisonIds: Collection<String>) = Event::prisonId.buildSpecForIn(prisonIds)
fun filterEventsByPrisonIds(vararg prisonIds: String) = filterEventsByPrisonIds(prisonIds.toList())

fun filterEventsByEventDateFrom(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForGreaterThanOrEqualTo(date.atStartOfDay())

fun filterEventsByEventDateUntil(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForLessThan(date.plusDays(1).atStartOfDay())
