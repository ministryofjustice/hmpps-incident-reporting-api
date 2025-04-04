package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.EventWithBasicReports
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByEventDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByEventDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByLocations
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByReference
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class EventService(
  private val eventRepository: EventRepository,
) {
  fun getEvents(
    reference: String? = null,
    locations: List<String> = emptyList(),
    eventDateFrom: LocalDate? = null,
    eventDateUntil: LocalDate? = null,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("eventDateAndTime").descending()),
  ): Page<EventWithBasicReports> {
    val specification = Specification.allOf(
      buildList {
        reference?.let { add(filterEventsByReference(reference)) }
        if (locations.isNotEmpty()) {
          add(filterEventsByLocations(locations))
        }
        eventDateFrom?.let { add(filterEventsByEventDateFrom(eventDateFrom)) }
        eventDateUntil?.let { add(filterEventsByEventDateUntil(eventDateUntil)) }
      },
    )
    return eventRepository.findAll(specification, pageable)
      .map { it.toDtoWithBasicReports() }
  }

  fun getEventById(id: UUID): EventWithBasicReports? {
    return eventRepository.findOneEagerlyById(id)
      ?.toDtoWithBasicReports()
  }

  fun getEventByReference(eventReference: String): EventWithBasicReports? {
    return eventRepository.findOneEagerlyByEventReference(eventReference)
      ?.toDtoWithBasicReports()
  }
}
