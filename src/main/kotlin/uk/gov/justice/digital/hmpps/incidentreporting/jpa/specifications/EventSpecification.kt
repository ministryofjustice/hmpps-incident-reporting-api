package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.time.LocalDate

fun filterEventsByReference(reference: String) = Event::eventReference.buildSpecForEqualTo(reference)

fun filterEventsByLocations(locations: Collection<String>) = Event::location.buildSpecForIn(locations)
fun filterEventsByLocations(vararg locations: String) = filterEventsByLocations(locations.toList())

fun filterEventsByEventDateFrom(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForGreaterThanOrEqualTo(date.atStartOfDay())

fun filterEventsByEventDateUntil(date: LocalDate) =
  Event::eventDateAndTime.buildSpecForLessThan(date.plusDays(1).atStartOfDay())
