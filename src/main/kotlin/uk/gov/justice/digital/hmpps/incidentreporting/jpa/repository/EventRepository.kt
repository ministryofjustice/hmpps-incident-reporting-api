package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event

@Repository
interface EventRepository : JpaRepository<Event, Long> {
  fun findOneByEventId(eventId: String): Event?

  @Query(value = "SELECT nextval('event_sequence')", nativeQuery = true)
  fun getNextEventId(): Long
}

fun EventRepository.generateEventId() = "IE-%016d".format(getNextEventId())
