package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import java.util.UUID

@Repository
interface EventRepository :
  JpaRepository<Event, UUID>,
  JpaSpecificationExecutor<Event> {
  @EntityGraph(value = "Event.eager", type = EntityGraph.EntityGraphType.LOAD)
  fun findOneEagerlyById(id: UUID): Event?

  fun findOneByEventReference(eventReference: String): Event?

  @EntityGraph(value = "Event.eager", type = EntityGraph.EntityGraphType.LOAD)
  fun findOneEagerlyByEventReference(eventReference: String): Event?

  @Query(value = "SELECT nextval('event_sequence')", nativeQuery = true)
  fun getNextEventReference(): Long
}

fun EventRepository.generateEventReference() = getNextEventReference().toString()
