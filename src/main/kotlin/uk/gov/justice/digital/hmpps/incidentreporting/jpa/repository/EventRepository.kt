package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<Event, UUID> {
  fun findOneByEventReference(eventReference: String): Event?

  @Query(value = "SELECT nextval('event_sequence')", nativeQuery = true)
  fun getNextEventReference(): Long
}

fun EventRepository.generateEventReference() = "IE-%016d".format(getNextEventReference())
