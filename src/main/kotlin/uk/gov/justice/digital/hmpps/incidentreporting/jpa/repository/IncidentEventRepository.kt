package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent

@Repository
interface IncidentEventRepository : JpaRepository<IncidentEvent, Long> {
  fun findOneByEventId(eventId: String): IncidentEvent?

  @Query(value = "SELECT nextval('incident_event_sequence')", nativeQuery = true)
  fun getNextEventId(): Long
}

fun IncidentEventRepository.generateEventId() = "IE-%016d".format(getNextEventId())
