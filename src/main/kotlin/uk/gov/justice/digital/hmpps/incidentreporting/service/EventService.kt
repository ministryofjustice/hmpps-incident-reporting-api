package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional(readOnly = true)
class EventService(
  private val eventRepository: EventRepository,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getEvents(
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("eventDateAndTime").descending()),
  ): Page<Event> {
    return eventRepository.findAll(pageable)
      .map { it.toDto() }
  }

  fun getEventById(id: UUID): Event? {
    return eventRepository.findById(id).getOrNull()
      ?.toDto()
  }

  fun getEventByReference(eventReference: String): Event? {
    return eventRepository.findOneByEventReference(eventReference)
      ?.toDto()
  }
}
